package org.example.mpj;

import org.example.util.PerformanceTimer;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Benchmark utility for measuring and comparing single-threaded vs distributed packet processing.
 */
public class PacketProcessingBenchmark {
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    private AtomicLong totalPackets = new AtomicLong(0);
    private DoubleAdder totalProcessingTime = new DoubleAdder();
    private DoubleAdder totalDistributionOverhead = new DoubleAdder();
    private ConcurrentHashMap<Integer, ProcessorStats> processorStats = new ConcurrentHashMap<>();
    private PerformanceTimer overallTimer = new PerformanceTimer();
    
    public void startBenchmark() {
        overallTimer.start();
        System.out.println("=== PACKET PROCESSING BENCHMARK STARTED ===");
        System.out.println("Timestamp: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
    
    public void endBenchmark() {
        overallTimer.stop();
        System.out.println("=== PACKET PROCESSING BENCHMARK COMPLETED ===");
        printFinalStatistics();
        exportResultsToFile();
    }
    
    public void recordPacketProcessed(PacketProcessingResult result) {
        totalPackets.incrementAndGet();
        totalProcessingTime.add(result.getProcessingTime());
        
        processorStats.computeIfAbsent(result.getProcessorRank(), k -> new ProcessorStats())
                     .recordPacket(result.getProcessingTime());
    }
    
    public void recordDistributionOverhead(double overhead) {
        totalDistributionOverhead.add(overhead);
    }
    
    private void printFinalStatistics() {
        long packets = totalPackets.get();
        double totalTime = overallTimer.getElapsedMillis();
        double avgProcessingTime = packets > 0 ? totalProcessingTime.sum() / packets : 0;
        double throughput = packets > 0 ? (packets / totalTime) * 1000 : 0; // packets per second
        
        System.out.println("\n=== FINAL BENCHMARK RESULTS ===");
        System.out.println("Total execution time: " + String.format("%.2f", totalTime) + " ms");
        System.out.println("Total packets processed: " + packets);
        System.out.println("Average processing time per packet: " + String.format("%.3f", avgProcessingTime) + " ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " packets/second");
        System.out.println("Total distribution overhead: " + String.format("%.2f", totalDistributionOverhead.sum()) + " ms");
        
        if (packets > 0) {
            double avgOverhead = totalDistributionOverhead.sum() / packets;
            System.out.println("Average distribution overhead per packet: " + String.format("%.3f", avgOverhead) + " ms");
        }
        
        System.out.println("\n=== PROCESSOR STATISTICS ===");
        processorStats.forEach((rank, stats) -> {
            System.out.println("Processor " + rank + ": " + stats);
        });
    }
    
    private void exportResultsToFile() {
        String filename = "benchmark_results_" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + ".csv";
        
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("Metric,Value\n");
            writer.write("Total Execution Time (ms)," + overallTimer.getElapsedMillis() + "\n");
            writer.write("Total Packets," + totalPackets.get() + "\n");
            writer.write("Average Processing Time (ms)," + (totalProcessingTime.sum() / totalPackets.get()) + "\n");
            writer.write("Throughput (packets/sec)," + ((totalPackets.get() / overallTimer.getElapsedMillis()) * 1000) + "\n");
            writer.write("Total Distribution Overhead (ms)," + totalDistributionOverhead.sum() + "\n");
            
            writer.write("\nProcessor,Packets Processed,Min Time (ms),Max Time (ms),Avg Time (ms)\n");
            processorStats.forEach((rank, stats) -> {
                try {
                    writer.write(rank + "," + stats.packetsProcessed + "," + 
                               stats.minTime + "," + stats.maxTime + "," + 
                               (stats.totalTime / stats.packetsProcessed) + "\n");
                } catch (IOException e) {
                    System.err.println("Error writing processor stats: " + e.getMessage());
                }
            });
            
            System.out.println("Benchmark results exported to: " + filename);
        } catch (IOException e) {
            System.err.println("Error exporting benchmark results: " + e.getMessage());
        }
    }
    
    /**
     * Compare single-threaded vs distributed processing performance
     */
    public static void comparePerformance(int numPackets, int numProcessors) {
        System.out.println("\n=== PERFORMANCE COMPARISON ===");
        System.out.println("Simulating processing of " + numPackets + " packets");
        System.out.println("Single-threaded vs " + numProcessors + " distributed processors");
        
        // Simulate single-threaded processing
        PerformanceTimer singleThreadTimer = new PerformanceTimer();
        singleThreadTimer.start();
        for (int i = 0; i < numPackets; i++) {
            // Simulate packet processing (1ms per packet)
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        singleThreadTimer.stop();
        
        // Calculate theoretical distributed processing time
        double distributedTime = (numPackets * 1.0) / numProcessors; // Assuming perfect parallelization
        double distributionOverhead = numPackets * 0.1; // Assume 0.1ms overhead per packet
        double totalDistributedTime = distributedTime + distributionOverhead;
        
        System.out.println("Single-threaded time: " + String.format("%.2f", singleThreadTimer.getElapsedMillis()) + " ms");
        System.out.println("Theoretical distributed time: " + String.format("%.2f", totalDistributedTime) + " ms");
        System.out.println("Theoretical speedup: " + String.format("%.2fx", singleThreadTimer.getElapsedMillis() / totalDistributedTime));
        System.out.println("Efficiency: " + String.format("%.1f%%", (singleThreadTimer.getElapsedMillis() / totalDistributedTime / numProcessors) * 100));
    }
    
    private static class ProcessorStats {
        long packetsProcessed = 0;
        double totalTime = 0.0;
        double minTime = Double.MAX_VALUE;
        double maxTime = 0.0;
        
        void recordPacket(double processingTime) {
            packetsProcessed++;
            totalTime += processingTime;
            if (processingTime < minTime) minTime = processingTime;
            if (processingTime > maxTime) maxTime = processingTime;
        }
        
        @Override
        public String toString() {
            double avgTime = packetsProcessed > 0 ? totalTime / packetsProcessed : 0;
            return String.format("Packets: %d, Avg: %.3fms, Min: %.3fms, Max: %.3fms", 
                               packetsProcessed, avgTime, minTime, maxTime);
        }
    }
}
