package org.example.mpj;

import org.example.core.SharedData;
import org.example.util.PerformanceTimer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Standalone benchmark runner to compare single-threaded vs simulated distributed processing.
 * This can be used to measure performance without needing the full MPJ environment.
 */
public class BenchmarkRunner {
    
    private static final int DEFAULT_PACKET_COUNT = 10000;
    private static final int DEFAULT_PROCESSOR_COUNT = 4;
    
    public static void main(String[] args) {
        int packetCount = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PACKET_COUNT;
        int processorCount = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PROCESSOR_COUNT;
        
        BenchmarkRunner runner = new BenchmarkRunner();
        runner.runComprehensiveBenchmark(packetCount, processorCount);
    }
    
    public void runComprehensiveBenchmark(int packetCount, int processorCount) {
        System.out.println("=== COMPREHENSIVE PACKET PROCESSING BENCHMARK ===");
        System.out.println("Packet count: " + packetCount);
        System.out.println("Processor count: " + processorCount);
        System.out.println();
        
        // Generate sample packet data
        String[] samplePackets = generateSamplePackets(packetCount);
        
        // Run single-threaded benchmark
        BenchmarkResult singleThreadResult = runSingleThreadedBenchmark(samplePackets);
        
        // Run multi-threaded simulation (simulates distributed processing)
        BenchmarkResult multiThreadResult = runMultiThreadedBenchmark(samplePackets, processorCount);
        
        // Print comparison
        printComparisonResults(singleThreadResult, multiThreadResult, processorCount);
        
        // Run theoretical performance comparison
        PacketProcessingBenchmark.comparePerformance(packetCount, processorCount);
    }
    
    private String[] generateSamplePackets(int count) {
        String[] packets = new String[count];
        String[] sampleIPs = {"192.168.1.10", "10.0.0.5", "172.16.0.20", "203.0.113.15", "198.51.100.30"};
        
        for (int i = 0; i < count; i++) {
            String ip = sampleIPs[i % sampleIPs.length];
            int port = 8080 + (i % 10);
            packets[i] = String.format("12:34:56.789 IP %s.%d > 192.168.1.1.8080: Flags [S], seq 123456", 
                                     ip, 50000 + (i % 1000));
        }
        
        return packets;
    }
    
    private BenchmarkResult runSingleThreadedBenchmark(String[] packets) {
        System.out.println("Running single-threaded benchmark...");
        
        PerformanceTimer timer = new PerformanceTimer();
        AtomicLong processedCount = new AtomicLong(0);
        double totalProcessingTime = 0.0;
        
        timer.start();
        
        for (String packet : packets) {
            PerformanceTimer packetTimer = new PerformanceTimer();
            packetTimer.start();
            
            // Simulate packet processing
            processPacketSingle(packet);
            
            packetTimer.stop();
            totalProcessingTime += packetTimer.getElapsedMillis();
            processedCount.incrementAndGet();
        }
        
        timer.stop();
        
        return new BenchmarkResult(
            "Single-threaded",
            timer.getElapsedMillis(),
            processedCount.get(),
            totalProcessingTime / processedCount.get(),
            processedCount.get() / (timer.getElapsedMillis() / 1000.0)
        );
    }
    
    private BenchmarkResult runMultiThreadedBenchmark(String[] packets, int threadCount) {
        System.out.println("Running multi-threaded benchmark with " + threadCount + " threads...");
        
        PerformanceTimer timer = new PerformanceTimer();
        AtomicLong processedCount = new AtomicLong(0);
        AtomicLong totalProcessingTime = new AtomicLong(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(packets.length);
        
        timer.start();
        
        for (String packet : packets) {
            executor.submit(() -> {
                try {
                    PerformanceTimer packetTimer = new PerformanceTimer();
                    packetTimer.start();
                    
                    // Simulate packet processing
                    processPacketDistributed(packet);
                    
                    packetTimer.stop();
                    totalProcessingTime.addAndGet((long)(packetTimer.getElapsedMillis() * 1000000)); // Convert to nanoseconds
                    processedCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        timer.stop();
        executor.shutdown();
        
        double avgProcessingTime = (totalProcessingTime.get() / 1000000.0) / processedCount.get(); // Convert back to milliseconds
        
        return new BenchmarkResult(
            "Multi-threaded (" + threadCount + " threads)",
            timer.getElapsedMillis(),
            processedCount.get(),
            avgProcessingTime,
            processedCount.get() / (timer.getElapsedMillis() / 1000.0)
        );
    }
    
    private void processPacketSingle(String packetLine) {
        // Simulate packet processing similar to original PacketConsumer
        String[] parts = packetLine.split("\\s+");
        if (parts.length >= 3 && "IP".equals(parts[1])) {
            String srcPort = parts[2];
            int idx = srcPort.lastIndexOf('.');
            if (idx > 0) {
                String srcIp = srcPort.substring(0, idx);
                
                // Simulate processing delay
                try {
                    Thread.sleep(1); // 1ms processing time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Update shared data
                SharedData.totalAttempted.incrementAndGet();
                SharedData.attemptsByIp
                        .computeIfAbsent(srcIp, k -> new AtomicLong(0))
                        .incrementAndGet();
            }
        }
    }
    
    private void processPacketDistributed(String packetLine) {
        // Same processing as single-threaded, but in parallel
        processPacketSingle(packetLine);
        
        // Add small distribution overhead
        try {
            Thread.sleep(0, 100000); // 0.1ms overhead
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void printComparisonResults(BenchmarkResult single, BenchmarkResult multi, int processorCount) {
        System.out.println("\n=== BENCHMARK COMPARISON RESULTS ===");
        System.out.println();
        
        System.out.printf("%-25s | %-15s | %-15s | %-20s | %-15s%n", 
                         "Method", "Total Time (ms)", "Packets", "Avg Time/Packet (ms)", "Throughput (pkt/s)");
        System.out.println("-".repeat(95));
        
        System.out.printf("%-25s | %-15.2f | %-15d | %-20.3f | %-15.2f%n",
                         single.method, single.totalTime, single.packetsProcessed, 
                         single.avgProcessingTime, single.throughput);
        
        System.out.printf("%-25s | %-15.2f | %-15d | %-20.3f | %-15.2f%n",
                         multi.method, multi.totalTime, multi.packetsProcessed, 
                         multi.avgProcessingTime, multi.throughput);
        
        System.out.println();
        
        // Calculate improvements
        double speedup = single.totalTime / multi.totalTime;
        double throughputImprovement = (multi.throughput - single.throughput) / single.throughput * 100;
        double efficiency = speedup / processorCount * 100;
        
        System.out.println("=== PERFORMANCE METRICS ===");
        System.out.printf("Speedup: %.2fx%n", speedup);
        System.out.printf("Throughput improvement: %.1f%%%n", throughputImprovement);
        System.out.printf("Parallel efficiency: %.1f%%%n", efficiency);
        
        if (speedup < processorCount * 0.7) {
            System.out.println("⚠️  Warning: Low parallel efficiency. Consider optimizing distribution overhead.");
        } else {
            System.out.println("✅ Good parallel efficiency achieved!");
        }
    }
    
    private static class BenchmarkResult {
        final String method;
        final double totalTime;
        final long packetsProcessed;
        final double avgProcessingTime;
        final double throughput;
        
        BenchmarkResult(String method, double totalTime, long packetsProcessed, 
                       double avgProcessingTime, double throughput) {
            this.method = method;
            this.totalTime = totalTime;
            this.packetsProcessed = packetsProcessed;
            this.avgProcessingTime = avgProcessingTime;
            this.throughput = throughput;
        }
    }
}
