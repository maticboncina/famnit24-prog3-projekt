package org.example.testing;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Consumes the M timestamps, injects a simulated delay,
 * measures latency, and writes results.
 */
public class TestConsumer implements Runnable {
    private final BlockingQueue<Long> queue;
    private final int total;
    private final long simulatedDelayMs;
    private final List<Double> latencies = new ArrayList<>();

    public TestConsumer(BlockingQueue<Long> queue, int total, long simulatedDelayMs) {
        this.queue            = queue;
        this.total            = total;
        this.simulatedDelayMs = simulatedDelayMs;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < total; i++) {
                long startNs = queue.take();
                if (simulatedDelayMs > 0) {
                    Thread.sleep(simulatedDelayMs);
                }
                long endNs = System.nanoTime();
                double latencyMs = (endNs - startNs) / 1_000_000.0;
                latencies.add(latencyMs);
            }

            // Summarize
            double sum = 0;
            for (double l : latencies) sum += l;
            double avg = sum / latencies.size();
            Collections.sort(latencies);
            double p99 = latencies.get((int)Math.floor(latencies.size() * 0.99));

            System.out.printf(
                    "Processed %,d packets with %d ms simulated delay:%n" +
                            "  • Average latency:    %.3f ms%n" +
                            "  • 99th-percentile:    %.3f ms%n",
                    total, simulatedDelayMs, avg, p99
            );

            // Write per-packet CSV
            String filename = String.format("latencies_%dms.csv", simulatedDelayMs);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
                writer.write("latency_ms\n");
                for (double l : latencies) {
                    writer.write(String.format("%.6f%n", l));
                }
            }
            System.out.println("Wrote detailed latencies to " + filename);
        } catch (Exception e) {
            System.err.println("Error in TestConsumer: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
