package org.example.mpj;

import mpi.MPI;
import org.example.core.SharedData;
import org.example.util.PerformanceTimer;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MPJ-based distributed packet processor that distributes packet processing
 * across multiple processes and measures processing delays.
 */
public class DistributedPacketProcessor {

    private static final int PACKET_TAG = 1;
    private static final int RESULT_TAG = 2;
    private static final int SHUTDOWN_TAG = 3;

    private int rank;
    private int size;
    private PerformanceTimer timer;
    private AtomicLong processedPackets;
    private ConcurrentHashMap<String, Long> processingTimes;

    public DistributedPacketProcessor() {
        this.timer = new PerformanceTimer();
        this.processedPackets = new AtomicLong(0);
        this.processingTimes = new ConcurrentHashMap<>();
    }

    /**
     * Initialize MPJ and start distributed packet processing
     */
    public void initialize(String[] args) {
        MPI.Init(args);
        this.rank = MPI.COMM_WORLD.Rank();
        this.size = MPI.COMM_WORLD.Size();

        System.out.println("Process " + rank + " of " + size + " initialized");

        if (rank == 0) {
            // Master process - distributes packets
            runMaster();
        } else {
            // Worker process - processes packets
            runWorker();
        }

        MPI.Finalize();
    }

    /**
     * Master process: distributes packets to workers and collects results
     */
    private void runMaster() {
        System.out.println("Master process starting packet distribution...");

        PacketDistributor distributor = new PacketDistributor(size - 1);
        ResultCollector collector = new ResultCollector();

        // Start distributor and collector threads
        Thread distributorThread = new Thread(distributor);
        Thread collectorThread = new Thread(collector);

        distributorThread.start();
        collectorThread.start();

        try {
            distributorThread.join();
            collectorThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Print final statistics
        printProcessingStatistics();
    }

    /**
     * Worker process: receives packets, processes them, and sends back results
     */
    private void runWorker() {
        System.out.println("Worker process " + rank + " ready for packet processing...");

        while (true) {
            // Receive packet data from master
            String[] packetData = new String[1];
            MPI.COMM_WORLD.Recv(packetData, 0, 1, MPI.OBJECT, 0, PACKET_TAG);

            // Check for shutdown signal
            if ("SHUTDOWN".equals(packetData[0])) {
                System.out.println("Worker " + rank + " shutting down...");
                break;
            }

            // Process the packet and measure time
            timer.start();
            PacketProcessingResult result = processPacket(packetData[0]);
            timer.stop();

            // Record processing time
            result.setProcessingTime(timer.getElapsedMillis());
            result.setProcessorRank(rank);

            processedPackets.incrementAndGet();

            // Send result back to master
            PacketProcessingResult[] resultArray = {result};
            MPI.COMM_WORLD.Send(resultArray, 0, 1, MPI.OBJECT, 0, RESULT_TAG);
        }

        System.out.println("Worker " + rank + " processed " + processedPackets.get() + " packets");
    }

    /**
     * Process a single packet and extract relevant information
     */
    private PacketProcessingResult processPacket(String packetLine) {
        PacketProcessingResult result = new PacketProcessingResult();
        result.setOriginalPacket(packetLine);
        result.setTimestamp(System.currentTimeMillis());

        try {
            // Parse packet similar to original PacketConsumer
            String[] parts = packetLine.split("\\s+");
            if (parts.length >= 3 && "IP".equals(parts[1])) {
                String srcPort = parts[2];
                int idx = srcPort.lastIndexOf('.');
                if (idx > 0) {
                    String srcIp = srcPort.substring(0, idx);
                    result.setSourceIP(srcIp);
                    result.setValid(true);

                    // Simulate some processing work
                    Thread.sleep(1); // 1ms processing delay

                    // Check if IP should be blocked (simple threshold check)
                    long attempts = SharedData.attemptsByIp
                            .computeIfAbsent(srcIp, k -> new AtomicLong(0))
                            .incrementAndGet();

                    if (attempts > SharedData.hardLimit) {
                        result.setShouldBlock(true);
                        SharedData.blockedIPs.add(srcIp);
                    }
                }
            }
        } catch (Exception e) {
            result.setValid(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * Print processing statistics for benchmarking
     */
    private void printProcessingStatistics() {
        System.out.println("\n=== DISTRIBUTED PACKET PROCESSING STATISTICS ===");
        System.out.println("Total processes: " + size);
        System.out.println("Master process: 1");
        System.out.println("Worker processes: " + (size - 1));

        // Additional statistics will be collected by ResultCollector
    }

    /**
     * Inner class to handle packet distribution
     */
    private class PacketDistributor implements Runnable {
        private final int numWorkers;
        private int currentWorker = 1; // Start with worker 1 (rank 0 is master)

        public PacketDistributor(int numWorkers) {
            this.numWorkers = numWorkers;
        }

        @Override
        public void run() {
            try {
                long packetsDistributed = 0;

                while (true) {
                    // Get packet from the shared queue
                    String packet = SharedData.packetLines.poll();
                    if (packet != null) {
                        // Send packet to next available worker
                        String[] packetArray = {packet};
                        MPI.COMM_WORLD.Send(packetArray, 0, 1, MPI.OBJECT, currentWorker, PACKET_TAG);

                        packetsDistributed++;
                        currentWorker = (currentWorker % numWorkers) + 1; // Round-robin distribution

                        if (packetsDistributed % 1000 == 0) {
                            System.out.println("Distributed " + packetsDistributed + " packets");
                        }
                    } else {
                        // No packets available, sleep briefly
                        Thread.sleep(10);
                    }
                }
            } catch (InterruptedException e) {
                // Send shutdown signal to all workers
                for (int i = 1; i <= numWorkers; i++) {
                    String[] shutdownSignal = {"SHUTDOWN"};
                    MPI.COMM_WORLD.Send(shutdownSignal, 0, 1, MPI.OBJECT, i, PACKET_TAG);
                }
            }
        }
    }

    /**
     * Inner class to collect processing results and calculate benchmarks
     */
    private class ResultCollector implements Runnable {
        private long totalResults = 0;
        private double totalProcessingTime = 0.0;
        private double minProcessingTime = Double.MAX_VALUE;
        private double maxProcessingTime = 0.0;
        private ConcurrentHashMap<Integer, Long> processingCountByWorker = new ConcurrentHashMap<>();

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    // Receive result from any worker
                    PacketProcessingResult[] resultArray = new PacketProcessingResult[1];
                    MPI.COMM_WORLD.Recv(resultArray, 0, 1, MPI.OBJECT, MPI.ANY_SOURCE, RESULT_TAG);

                    PacketProcessingResult result = resultArray[0];
                    processResult(result);

                    totalResults++;

                    if (totalResults % 1000 == 0) {
                        printIntermediateStats();
                    }
                }
            } catch (Exception e) {
                System.err.println("ResultCollector error: " + e.getMessage());
            }
        }

        private void processResult(PacketProcessingResult result) {
            double processingTime = result.getProcessingTime();
            totalProcessingTime += processingTime;

            if (processingTime < minProcessingTime) {
                minProcessingTime = processingTime;
            }
            if (processingTime > maxProcessingTime) {
                maxProcessingTime = processingTime;
            }

            processingCountByWorker.merge(result.getProcessorRank(), 1L, Long::sum);
        }

        private void printIntermediateStats() {
            double avgProcessingTime = totalProcessingTime / totalResults;
            System.out.println("\n--- Intermediate Processing Stats ---");
            System.out.println("Total packets processed: " + totalResults);
            System.out.println("Average processing time: " + String.format("%.3f", avgProcessingTime) + " ms");
            System.out.println("Min processing time: " + String.format("%.3f", minProcessingTime) + " ms");
            System.out.println("Max processing time: " + String.format("%.3f", maxProcessingTime) + " ms");
            System.out.println("Processing distribution by worker: " + processingCountByWorker);
        }
    }
}
