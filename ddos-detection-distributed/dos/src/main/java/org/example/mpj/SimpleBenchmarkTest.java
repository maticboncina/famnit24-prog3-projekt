package org.example.mpj;

import org.example.util.PerformanceTimer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple test runner to demonstrate the benchmarking concept without MPJ dependencies.
 * This shows how to measure packet processing delays in single-threaded vs multi-threaded scenarios.
 */
public class SimpleBenchmarkTest {
    
    public static void main(String[] args) {
        System.out.println("=== SIMPLE PACKET PROCESSING BENCHMARK TEST ===");
        
        int packetCount = 1000; // Smaller test size
        int threadCount = 4;
        
        // Generate test data
        String[] testPackets = generateTestPackets(packetCount);
        
        // Run single-threaded test
        System.out.println("\n1. Testing single-threaded processing...");
        BenchmarkResult singleResult = testSingleThreaded(testPackets);
        
        // Run multi-threaded test
        System.out.println("\n2. Testing multi-threaded processing with " + threadCount + " threads...");
        BenchmarkResult multiResult = testMultiThreaded(testPackets, threadCount);
        
        // Display results
        printResults(singleResult, multiResult, threadCount);
    }
    
    private static String[] generateTestPackets(int count) {
        String[] packets = new String[count];
        String[] ips = {"192.168.1.10", "10.0.0.5", "172.16.0.20", "203.0.113.15"};
        
        for (int i = 0; i < count; i++) {
            String ip = ips[i % ips.length];
            packets[i] = String.format("12:34:56.789 IP %s.%d > 192.168.1.1.8080: Flags [S]", 
                                     ip, 50000 + i);
        }
        return packets;
    }
    
    private static BenchmarkResult testSingleThreaded(String[] packets) {
        PerformanceTimer timer = new PerformanceTimer();
        AtomicLong processed = new AtomicLong(0);
        double totalProcessingTime = 0.0;
        
        timer.start();
        
        for (String packet : packets) {
            PerformanceTimer packetTimer = new PerformanceTimer();
            packetTimer.start();
            
            // Simulate packet processing
            processPacket(packet);
            
            packetTimer.stop();
            totalProcessingTime += packetTimer.getElapsedMillis();
            processed.incrementAndGet();
        }
        
        timer.stop();
        
        return new BenchmarkResult(
            timer.getElapsedMillis(),
            processed.get(),
            totalProcessingTime / processed.get()
        );
    }
    
    private static BenchmarkResult testMultiThreaded(String[] packets, int threadCount) {
        PerformanceTimer timer = new PerformanceTimer();
        AtomicLong processed = new AtomicLong(0);
        AtomicLong totalProcessingTimeNanos = new AtomicLong(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(packets.length);
        
        timer.start();
        
        for (String packet : packets) {
            executor.submit(() -> {
                try {
                    PerformanceTimer packetTimer = new PerformanceTimer();
                    packetTimer.start();
                    
                    processPacket(packet);
                    
                    packetTimer.stop();
                    totalProcessingTimeNanos.addAndGet((long)(packetTimer.getElapsedMillis() * 1_000_000));
                    processed.incrementAndGet();
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
        
        double avgProcessingTime = (totalProcessingTimeNanos.get() / 1_000_000.0) / processed.get();
        
        return new BenchmarkResult(
            timer.getElapsedMillis(),
            processed.get(),
            avgProcessingTime
        );
    }
    
    private static void processPacket(String packetLine) {
        // Simulate packet parsing and processing
        String[] parts = packetLine.split("\\s+");
        if (parts.length >= 3 && "IP".equals(parts[1])) {
            String srcInfo = parts[2];
            int idx = srcInfo.lastIndexOf('.');
            if (idx > 0) {
                String srcIp = srcInfo.substring(0, idx);
                
                // Simulate processing work (1ms delay)
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Simulate some analysis work
                srcIp.hashCode(); // Simple computation
            }
        }
    }
    
    private static void printResults(BenchmarkResult single, BenchmarkResult multi, int threadCount) {
        System.out.println("\n=== BENCHMARK RESULTS ===");
        System.out.println();
        
        System.out.printf("%-20s | %-15s | %-15s | %-20s | %-15s%n", 
                         "Method", "Total Time (ms)", "Packets", "Avg Time/Packet (ms)", "Throughput (pkt/s)");
        System.out.println("-".repeat(90));
        
        double singleThroughput = single.packetsProcessed / (single.totalTime / 1000.0);
        System.out.printf("%-20s | %-15.2f | %-15d | %-20.3f | %-15.2f%n",
                         "Single-threaded", single.totalTime, single.packetsProcessed, 
                         single.avgProcessingTime, singleThroughput);
        
        double multiThroughput = multi.packetsProcessed / (multi.totalTime / 1000.0);
        System.out.printf("%-20s | %-15.2f | %-15d | %-20.3f | %-15.2f%n",
                         "Multi-threaded", multi.totalTime, multi.packetsProcessed, 
                         multi.avgProcessingTime, multiThroughput);
        
        System.out.println();
        
        // Calculate performance metrics
        double speedup = single.totalTime / multi.totalTime;
        double throughputImprovement = (multiThroughput - singleThroughput) / singleThroughput * 100;
        double efficiency = speedup / threadCount * 100;
        
        System.out.println("=== PERFORMANCE ANALYSIS ===");
        System.out.printf("Speedup: %.2fx%n", speedup);
        System.out.printf("Throughput improvement: %.1f%%%n", throughputImprovement);
        System.out.printf("Parallel efficiency: %.1f%%%n", efficiency);
        
        System.out.println();
        if (efficiency > 70) {
            System.out.println("✅ Excellent parallel efficiency!");
        } else if (efficiency > 50) {
            System.out.println("✓ Good parallel efficiency.");
        } else {
            System.out.println("⚠️  Lower efficiency - consider optimizing or reducing thread count.");
        }
        
        System.out.println("\n=== DELAY ANALYSIS ===");
        System.out.printf("Single-threaded avg delay: %.3f ms per packet%n", single.avgProcessingTime);
        System.out.printf("Multi-threaded avg delay: %.3f ms per packet%n", multi.avgProcessingTime);
        double delayReduction = (single.avgProcessingTime - multi.avgProcessingTime) / single.avgProcessingTime * 100;
        System.out.printf("Delay reduction: %.1f%%%n", delayReduction);
    }
    
    private static class BenchmarkResult {
        final double totalTime;
        final long packetsProcessed;
        final double avgProcessingTime;
        
        BenchmarkResult(double totalTime, long packetsProcessed, double avgProcessingTime) {
            this.totalTime = totalTime;
            this.packetsProcessed = packetsProcessed;
            this.avgProcessingTime = avgProcessingTime;
        }
    }
}
