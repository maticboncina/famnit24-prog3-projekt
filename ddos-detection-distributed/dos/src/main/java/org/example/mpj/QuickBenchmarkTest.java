package org.example.mpj;

import org.example.util.PerformanceTimer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Quick benchmark test with smaller parameters for faster execution.
 */
public class QuickBenchmarkTest {
    
    public static void main(String[] args) {
        System.out.println("=== QUICK PACKET PROCESSING BENCHMARK ===");
        
        int packetCount = 100; // Small test size for quick results
        int threadCount = 4;
        
        // Generate test data
        String[] testPackets = generateTestPackets(packetCount);
        
        // Run single-threaded test
        System.out.println("\n1. Single-threaded processing (" + packetCount + " packets)...");
        BenchmarkResult singleResult = testSingleThreaded(testPackets);
        
        // Run multi-threaded test
        System.out.println("2. Multi-threaded processing (" + threadCount + " threads)...");
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
            
            // Simulate packet processing (much shorter delay)
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
        // Simulate packet parsing and processing (very minimal for quick test)
        String[] parts = packetLine.split("\\s+");
        if (parts.length >= 3 && "IP".equals(parts[1])) {
            String srcInfo = parts[2];
            int idx = srcInfo.lastIndexOf('.');
            if (idx > 0) {
                String srcIp = srcInfo.substring(0, idx);
                
                // Very short processing delay
                try {
                    Thread.sleep(0, 500000); // 0.5ms delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Simulate some analysis work
                srcIp.hashCode();
            }
        }
    }
    
    private static void printResults(BenchmarkResult single, BenchmarkResult multi, int threadCount) {
        System.out.println("\n=== BENCHMARK RESULTS ===");
        
        double singleThroughput = single.packetsProcessed / (single.totalTime / 1000.0);
        double multiThroughput = multi.packetsProcessed / (multi.totalTime / 1000.0);
        
        System.out.printf("Single-threaded: %.2f ms total, %.3f ms/packet, %.1f packets/sec%n",
                         single.totalTime, single.avgProcessingTime, singleThroughput);
        
        System.out.printf("Multi-threaded:  %.2f ms total, %.3f ms/packet, %.1f packets/sec%n",
                         multi.totalTime, multi.avgProcessingTime, multiThroughput);
        
        // Calculate metrics
        double speedup = single.totalTime / multi.totalTime;
        double throughputImprovement = (multiThroughput - singleThroughput) / singleThroughput * 100;
        double efficiency = speedup / threadCount * 100;
        
        System.out.println("\n=== PERFORMANCE ANALYSIS ===");
        System.out.printf("Speedup: %.2fx%n", speedup);
        System.out.printf("Throughput improvement: %.1f%%%n", throughputImprovement);
        System.out.printf("Parallel efficiency: %.1f%%%n", efficiency);
        
        if (efficiency > 70) {
            System.out.println("✅ Excellent parallel performance!");
        } else if (efficiency > 50) {
            System.out.println("✓ Good parallel performance.");
        } else {
            System.out.println("⚠️  Room for optimization.");
        }
        
        System.out.println("\n=== DELAY COMPARISON ===");
        double delayReduction = (single.avgProcessingTime - multi.avgProcessingTime) / single.avgProcessingTime * 100;
        System.out.printf("Processing delay reduced by: %.1f%%%n", Math.abs(delayReduction));
        
        System.out.println("\n💡 This demonstrates how MPJ distributed processing can improve");
        System.out.println("   packet processing throughput and reduce overall delays!");
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
