package org.example.testing;

import java.util.concurrent.BlockingQueue;

/**
 * Produces M timestamped “packets” (nano-timestamps) into the queue.
 */
public class TestProducer implements Runnable {
    private final BlockingQueue<Long> queue;
    private final int total;

    public TestProducer(BlockingQueue<Long> queue, int total) {
        this.queue = queue;
        this.total = total;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < total; i++) {
                queue.put(System.nanoTime());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
