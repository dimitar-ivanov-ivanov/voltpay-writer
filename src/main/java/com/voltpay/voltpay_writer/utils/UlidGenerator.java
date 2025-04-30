package com.voltpay.voltpay_writer.utils;

import com.github.f4b6a3.ulid.UlidCreator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UlidGenerator {

    // Extracting this outside of the method, otherwise we'll call it for every ULID
    // There is no caching so we'll get the env variable every time
    private static String NODE_ID = System.getenv("NODE_ID");

    private UlidGenerator() {
        // no instances
    }

    public static String generateUlid() {
        try {
            // Append the node id to the end of the ULID to make sure that there is ZERO chance to ever create the same ULID accross instances.
            // There was a astronomically low chance before, but this ensures that chance is zero
            // The node is MAX 3 symbols, they are all decimal and the number of the instance, so no more instances than 999

            return UlidCreator.getMonotonicUlid().toString() + "-" + NODE_ID;
        } catch (Exception ex) {
            log.error("Couldn't generate ULID.", ex);
            throw ex;
        }
    }
}
