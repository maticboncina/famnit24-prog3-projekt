package org.example.core;

import java.io.IOException;

public class PacketCapture {
    public static void startPacketCapture() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                "sudo", "tcpdump", "-l", "-n", "-i", "any",
                "port", "8080", "and", "(tcp-syn|tcp-ack)!=0"
        );
        pb.redirectErrorStream(true);
        SharedData.tcpdumpProcess = pb.start();
    }
}
