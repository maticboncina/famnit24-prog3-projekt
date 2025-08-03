package org.example.testing;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Entry point:
 *   java org.example.testing.TestingMain <numPackets> <simulatedDelayMs>
 */
public class TestingMain {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println(
                    "Usage: java org.example.testing.TestingMain <numPackets> <simulatedDelayMs>"
            );
            System.exit(1);
        }

        int M       = Integer.parseInt(args[0]);
        long delay  = Long.parseLong(args[1]);
        BlockingQueue<Long> queue = new ArrayBlockingQueue<>(M);

        Thread consumer = new Thread(new TestConsumer(queue, M, delay), "TestConsumer");
        Thread producer = new Thread(new TestProducer(queue, M),       "TestProducer");

        // Start & wait
        consumer.start();
        producer.start();
        producer.join();
        consumer.join();
    }
}
