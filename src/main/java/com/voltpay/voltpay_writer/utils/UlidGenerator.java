package com.voltpay.voltpay_writer.utils;

import com.github.f4b6a3.ulid.UlidCreator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UlidGenerator {

    private UlidGenerator() {
        // no instances
    }

    public static String generateUlid() {
        try {
            // Append the node to the end of the ULID to make sure that there is ZERO chance to ever create the same ULID accross instances.
            // There was a astronomically low chance before, but this ensures that chance is zero
            // The node is MAX 3 symbols, they are all decimal an the number of the instance, so no more instances than 999
            String nodeId = System.getenv("NODE_ID");
            return UlidCreator.getMonotonicUlid().toString() + "-" + nodeId;
        } catch (Exception ex) {
            log.error("Couldn't generate ULID.", ex);
            throw ex;
        }
    }
}
