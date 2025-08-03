package org.example.mpj;

import org.example.core.PacketCapture;
import org.example.core.PacketConsumer;
import org.example.core.HttpServerStarter;

/**
 * Main class for running MPJ-based distributed packet processing.
 * This should be launched with MPJ runtime environment.
 * 
 * Usage: mpjrun.sh -np <number_of_processes> java org.example.mpj.MPJDistributedMain
 */
public class MPJDistributedMain {
    
    public static void main(String[] args) {
        System.out.println("Starting MPJ Distributed Packet Processing System...");
        
        try {
            // Initialize the distributed packet processor
            DistributedPacketProcessor processor = new DistributedPacketProcessor();
            
            // Start packet capture and HTTP server (only on master process)
            // Note: In MPJ, rank 0 is typically the master
            if (isMasterProcess(args)) {
                startInfrastructure();
            }
            
            // Initialize MPJ and start processing
            processor.initialize(args);
            
        } catch (Exception e) {
            System.err.println("Error in MPJ distributed processing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check if this is the master process (rank 0)
     */
    private static boolean isMasterProcess(String[] args) {
        // This is a simplified check - in real MPJ environment,
        // you would check MPI.COMM_WORLD.Rank() after MPI.Init()
        return true; // For now, assume we start infrastructure on all processes
    }
    
    /**
     * Start the necessary infrastructure (packet capture, HTTP server)
     */
    private static void startInfrastructure() {
        try {
            System.out.println("Starting packet capture...");
            PacketCapture.startPacketCapture();
            
            System.out.println("Starting packet consumer...");
            PacketConsumer.startPacketConsumer();
            
            System.out.println("Starting HTTP server...");
            HttpServerStarter.startHttpServer();
            
            System.out.println("Infrastructure started successfully.");
            
        } catch (Exception e) {
            System.err.println("Error starting infrastructure: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
