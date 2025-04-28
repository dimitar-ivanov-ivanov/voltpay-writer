## Voltpay Payment Writer

This Service is responsible for writing payments for Voltpay.
This is an experiment for me to try to make a system that writes 100K transactions per second.
The reading logic is offloaded to a separate service to ensure that there is no contention for CPU/Resources 
between reading and writing, so both are in separate services and can independently scaled and deployed.
Neither of these microservices know of each other's existence as they communicate through Kafka topic.
Writer writes to payment_read_topic after we have commited successfully changes to the Writer DB.
This means that the Writer does NOT wait for the Reader to persist the changes, so if you write and after 
1 nanosecond try to read the data you probably can't as the data for reading is eventually consistent.
There is an idempotency check every time we try to process a message to ensure we don't reprocessed already processed messages.
The reprocessing can happen because of rebalancing, restarts, retries etc.
There is an idempotency job that deletes idempotency records older than one week.
Kafka consumer should consume messages in batches and we do ONE DB commit for the entire batch, also we do ONE OFFSET Commit/acknowledgement to Kafka.
After successful write publish to payment_email topic which will be consumed by payment email service and it will
send emails to the customer that made the successful payment.

# Architecture
![img_2.png](img_2.png)
# Kafka 
  - Consume messages from payment_writer_topic.
  - Check for idempotency of the message, just in case some messages are re-emitted OR the consumer offset gets moved back
  - The topic has 100 partitions with 2 replicas and 1 day retention
  - There are 2 Kafka brokers as a start, more can be added in the future
  - In the perf project there is an API for producing warmup events, very important to do before testing to ensure no performance degradation during big loads.
  - If there are no warmup events during the first performance test the results are disastrous, it was 330 tps, on the third run it was 3300.
  - The MESSAGE_ID of the messages is the ACCOUNT_ID which ensures transactions for one account are written sequentially so NO race conditions as all of the messages for that ACCOUND_ID go into one partition and processing for a single partition is sequential.
  - Consumer should Batch consumer and take messages in batches, the messages will then be grouped by key in Map<Key,List<Value>>
  - We are using custom Serializer/Deserializer that filer out NON-NULL, NON-EMPTY fields to reduce message size.
  - IF a message fails we put only that message to the dead letter and acknowledge the batch and move forward
  - Reprocessing strategy for failed messages -> Re-emit in the original topic.

# Database 
  - PostgreSQL is the chosen DB for it reliability and flexibilty.
  - We want our DB schema to be normalized as possible.
  - This allows us to not have contention on row level when writing because we are writing to many tables at once.
  - Idempotency table consists of Message_ID, DATE
  - NO INDEXES on the main WRITER tables besides Primary Key! Each index slows down updates and could slow down create significantly, rebalancing an index is costly.
  - This makes it possible to insert two records for ID X in metadata and notes, the app will have to ensure that doesn't happen.
  - The IDs for the payments will be ULIDS instead of UUID
    because ULIDS are still unique, can be converted to timestamp to see when they were created, can be sorted lexicographically and are better for indexing
  - ULID generation will be done in the app, there is an astronomically low chance to create the same ULID twice, this case will be handled 
  - I'd rather avoid a DB bottleneck for the ULID generation in the DB, also generating it in the app is more flexible as in the future we can choose to write it to another storage
  - Why collison could happen: ULID = 48 bits of timestamp + 80 bits of randomness the odds of a collision happening is 2^80 (1 in 1.2 quintillion)
  - NO FOREIGN KEY constraints between the tables, it's more optimal to keep things loose and ensure on app level that ID for the records is consistent in the different tables.
  - Tables to be PARTITIONED, it will reduce contention on the same table and distributed writes to different tables (partitions)
  - Tables to be partitioned on a MONTHLY basis by CREATED_AT column
  - One downside of partitioning is that the PRIMARY KEY has to also include CREATED_AT column to ensure uniqueness across partitions, making the index bigger
  - pgpartman(https://github.com/pgpartman/pg_partman) extension will be used to created and manage partitions, new partitions for the next 2 months will be created at the start of every month
  - For the ID it will be ULID-XXX where XXX is the node ID, even though it's almost impossible to have a collision if we have millions of transactions per secon and hundres of instances of the service it COULD happen theorically
  - Adding the node id ensures that it won't happen as in one process(instance) we are guaranteed to generate unique ULIDs
  - DBeaver is the chosen app for Database UI

# Liquibase
  - The chosen approach for version control of the database 
  - Easy to use and we have rollbacks it necessary 
  - We have an audit trail
  - It's easy to integrate in the CI/CD and it ensures consistent DB changes across all environments
  - Liquibase doesn't directly support schema creation or the management of extensions so they have to be done manually 
  - otherwise liquibase will throw exception for not existing schema when you execute the scripts, schema CANNOT be created in liquibase
  - ChangeSet Ids to be order ascendingly from file to file, if file 1 ends at changeset 3, file 2 needs to start from change 4 to ensure that when querying databasechangelog we can easily track the change
  - commands
     - ``gradle update`` -> apply changes
     - ``gradle clearChecksums update`` -> if you change a changelog already execute it will throw an exception when you reexecute it   
     - ``gradle rollbackCount -PliquibaseCommandValue=1`` -> very important for rolling back ONE change
     - ``SELECT * FROM databasechangelog ORDER BY dateexecuted DESC;`` -> to monitor when a change was executed

# Functional Monitoring - TODO
  - TBD but most likely 
  - Kafka UI -> useful for monitoring individual Kafka messages, Consumer lag, moving offset and truncating/deleting topics
  - Prometheus + Grafana -> Request Rate, Latency, Error Rates, Consumer Lag 
  - Kafka Lag Exporter -> Monitor Kafka lag, partition throughput, broker performance, consider Kafdrop also 
  - OpenTelemetry + Zipkin -> Trace full lifecycle of flows, helps to detect bottlenecks, slow queries
  - Loki - used for general logs with correlation IDs

# Non-Functional Monitoring
 - Node Exporter - Infra monitoring for CPU, RAM, Disk, network etc
 - VisualVM - Java process monitoring: heap usage and dumps, thread dumps, thread dumps to be analyzed with https://spotify.github.io
 - JConsole - Java memory, CPU, Threads
 - JProfiler - same as above two

# Alerts - TODO 
  - High Error Rate
  - High Kafka Consumer Lag
  - Latency Spikes
  - Low throughput 
  - DB slow queries 
  - Out of memory errors

# General Stuff
- Code Quality
     - Checkstyle is used to enforce standards for code quality
     - use ``gradle checkstyleMain`` to use
     - checkstyle rules can be found in ``config/checkstyle/checkstyle.xml``
- Functional Tests 
     - Unit Tests 
     - Integration Tests

# Other ideas that were considered and DROPPED

1. **Use trigger to persist data to reader table**
   - The trigger is persisting in the read table records one by one, consumer for reads can commit in batches 
   - having a consumer is more flexible, we can choose in the future to commit the data to another storage i.e to a separate database just for reading 
   - monitoring a trigger behavior is a big question mark ?? However monitoring a Java Consumer for Kafka is much more straight-forward
   - also I don't know how to test the trigger, but I do know how to write a unit/integration test for Kafka consumer written in Java.
   - for the consumer we can rely on Kafka retries(3 by default) if the trigger fails once it will have to go to a Dead Letter Table (not bad but you have more dependency on the DB instead of Kafka)
2. **Use UUID for Write DB and ULID for READ DB**
    - Initially the idea was that we don't need indexes which greatly benefit from ULID and we can just use UUID, but then use ULIDs for READs for faster index node insertion.
    - I dropped this because I'll have to do some magic converstions which will slow down the system 
    - Better to use ULIDs because even though they have no benefit for Write they have benefit for Read and the IDs for records are the same in both systems so no mappings needed
3. **Remove the index on PRIMARY KEY on "payment_core" table to speed up writes** 
   - Ultimately I decided against this, because we will use ULID for the ID which shows good performance for the writes i.e there isn't that much rebalancing of the index and also this index will be beneficial for updates
4. **Generate ULID in the DB using https://github.com/andrielfn/pg-ulid.git**
   - Ultimately I decided to generate it on app level 
   - I already have a created_at column and I don't see perfect ULID sort order
   - I'd rather avoid a DB bottleneck for the ULID generation in the DB, also generating it in the app is more flexible as in the future we can choose to write it to another storage
5. **Use Hash based partitioning for tables**
   - I wanted to use this initially because partitioning by months, days, hours seemed like it would lead to too many partitions 
   - problem with hash based partitioning is that when you declare how many partitions you want you can't increase them and repartition the data 
   - I'll have to manually repartition so make a new table with more partitions, start writing to it, migrate the data to it, route client to the new table and then delete the old table.
   - that seemed like too much maitenance work so I dropped this idea.
6. **Use ```@Transactional``` for the consumer** 
   - Unfortunately the transaction is commited after the method execution and it's possble that we have failed events that won't be persisted 
   - I need to know all of the successfully persisted events so that I produce them to the reader-topic, otherwise I have to publish also the failed events and there will be a difference between WRITE and READ db
   - so doing manual transactions to the DB using EntityManager was preferrable
   - ALSO "Transaction silently rolled back because it has been marked as rollback-only" happens when one event fails so doing a partial commit for the sucessful events of a batch is impossible
7. **Caching ULIDs at the start of the APP** 
   - This was an idea to improve ULID generation by creating them upfront and not every time we process a record.
   - And then we fetch ulids from the cache when we process a record
   - The main problem here is that we'll have a bigger memory footprint
   - When we run out of ulids we'll have to generate them again which might cause a delay and timeouts with kafka or rebalancing for the consumer 
   - We'll have at least 100 threads using that structure so it has to be thread-safe, but more importantly it adds another lock that the threads will have to wait for 
   - Also that lock is just slowing down threads, causing more CPU context switching

# How to Set up Locally

# Kafka 
 - As mentioned we need to create 4 Brokers and 1 Topic for writing with 100 partitions
 - The other topics will be created by the Apps that will use them 
 - [START] ``docker compose up -d``
 - [TOPIC CREATION] ``docker exec -it kafka1 kafka-topics --create --topic write-topic --bootstrap-server kafka1:29092,kafka2:29093,kafka3:29094,kafka4:29095 --partitions 100 --replication-factor 1``
 - [VERIFY] ``docker exec kafka1 kafka-topics --list --bootstrap-server kafka1:29092``
 - [TOPIC DELETION] ``docker exec -it kafka1 kafka-topics --delete --topic write-topic --bootstrap-server kafka1:29092,kafka2:29093,kafka3:29094,kafka4:29095``
 - [END] ``docker compose down``
 - [REMOVE VOLUMES] ``docker volume rm voltpay-writer_zookeeper-data voltpay-writer_zookeeper-log voltpay-writer_kafka1-data voltpay-writer_kafka2-data voltpay-writer_kafka3-data voltpay-writer_kafka4-data``
 - [ALTER PARTITIONS ON TOPIC] ``docker exec -it kafka1 kafka-topics --alter --topic write-topic --partitions 150 --bootstrap-server kafka1:29092,kafka2:29093,kafka3:29094,kafka4:29095``
 - **[VERIFY EVENTS PRODUCED FOR READER TOPIC]**
 - ``docker exec -it kafka1 bash``
 - ``cd ../../bin`` -> folder with scripts 
 - ``kafka-console-consumer --bootstrap-server kafka1:29092 --topic read-topic --property print.key=true --property print.timestamp=true --property print.partition=true --from-beginning``
 - OR ``docker exec kafka1 kafka-console-consumer --bootstrap-server kafka1:29092 --topic read-topic --property print.key=true --property print.timestamp=true --property print.partition=true --from-beginning``

# PostgreSQL
 - ``docker compose up -d``
 - add liquibase dependencies/plugin to build.gradle
 - ``gradle build --refresh-dependencies``
 - ``gradle update`` OR ``gradle clearChecksums update``(if there is a problem with checksums) - to trigger liquibase scripts
 - ``docker exec -it postgres bash``
 - [CONNECT TO DB] ``psql -U user -d write_db``
 - ``CREATE SCHEMA IF NOT EXISTS write;`` -> general setup for the schema the app will be using
 - **[OVERRIDE POSSIBLE NUMBER OF CONNECTIONS]**
 - ``docker exec -it postgres bash``
 - ``cd /var/lib/postgresql/data``
 - ``apt-get update``
 - ``apt-get install nano``
 - ``nano postgresql.conf``
 - ``change max_connections to whatever you want``

# Set up PG Partman
 - ``docker exec -it postgres bash``
 - ``apt-get update``
 - ``apt-get install postgresql-16-partman``
 - ``apt-get install git``
 - ``apt-get install make``
 - ``apt-get install -y gcc make postgresql-server-dev-16 libxml2-dev``
 - ``git clone https://github.com/pgpartman/pg_partman.git``
 - ``cd pg_partman``
 - ``make``
 - ``make install``
 - [CONNECT TO DB] ``psql -U user -d write_db``
 - ``CREATE SCHEMA IF NOT EXISTS partman;``
 - ``CREATE EXTENSION IF NOT EXISTS pgcrypto SCHEMA partman;``
      - pgcrypto is needed to generate UUIDs names for child partitions, indexes etc  
 - ``CREATE EXTENSION IF NOT EXISTS pg_partman SCHEMA partman;``
 - ``SELECT * FROM pg_available_extensions WHERE name = 'pg_partman';`` to verify

## Performance Test Tools 
 - Jmeter 
    - download Binaries: https://jmeter.apache.org/download_jmeter.cgi
    - latest test plan can be  found here: https://github.com/dimitar-ivanov-ivanov/voltpay-perf-test/blob/main/jmeter_test_plan.jmx

## Performance Test Results
 - All tests to be conducted locally on my machine 
   - CPU Intel(R) Core(TM) i7-14700HX - 20 CORES
   - Memory - 32GB RAM
   - GPU NVIDIA GeForce RTX 4060 Laptop GPU
 - First RUN: Consuming 15k Records (04/23/2025)
   - **DISASTER**, about 200 records per second, this is because there are NO warmup events 
 - 3rd RUN: Consuming 25k Records (04/23/2025)
   - Results are warmup (first and second run)
   - Average records processed per second: 3,272.9
 - 4th RUN: Consuming 50K Records (04/23/2025)
   - Enable G1 garbage collector, increased consumer threads, increased DB connections to match consumer threads
   - Majority of early records stayed in the 60–80 ms sweet spot → ~6,700–8,300 records/sec
   - A few tiny spikes early on (27 ms, 30 ms, etc.)
   - Gradual shift into 100–200 ms, then uphill climb into the seconds
   - End of the run: frequent 4,000–6,000 ms durations
   - 🧨 One massive outlier at 11,600 ms
   - There is a repeatable “slowdown curve” over time:
   - Starts fast (steady sub-100ms range)
   - Middle section goes through bumpy ramps (100ms → 3000ms)
   - Final chunk gets choppy and heavy (4000ms+)
   - This may point to memory pressure, I/O bottlenecks, queueing, or GC buildup!
   - ![img.png](img.png)
  - 5th RUN - 25k records (04/24/2025)
    - No Stop the world events, slow performance in generateUlid() because we take NODE_ID from environment for every record instead of taking once
    - Added 2 new brokers, and an additional service instance with 50 new consumer threads
    - Average records processed per second: 220 tps 
    - Maximum records per second: 10k
    - Minimum records per second: 59
    - I think I'm starting to hit hardware issues, still pretty good considering I am running this on my local machine.
  - 6th RUN - 25k records (04/24/2025)
    - Removed ``parallelStream`` and it greatly increased throughput 
    - Average records processed per second: 1,233.34 
  - 7th RUN (04/25/2025)
    - reduce batch size to 250
    - warmup with 3k,5k,15k, then load testing with 5k,10k,15k,25k
    - results are from 30k records
    - A lot more consistent average, we were about to hit **5400 records processed per second**
  - 8th RUN (04/25/2025)
    - make topic have 150 partitions
    - add another instance of the writer(150 consumer threads total)
    - increase the possible connections the DB can take to 150 (its 100 by default)
    - warmup with 5k, 15k, 25k records, load testing with 5k, 10k, 15k, 20k
    - for 25k records -> average: 3,186 records/second
    - for 30k records -> average: 2,792 records/second
    - for 30k records again -> 2,836 records/second
    - overall it got worse because of resource contention for the consumer threads(150) and because of Kafka rebalancing
    - this could yield better results on machine stronger than mine
