package org.example.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicLong;

public class PacketConsumer {
    public static void startPacketConsumer() {
        // Reader thread
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(SharedData.tcpdumpProcess.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    SharedData.packetLines.put(line);
                }
            } catch (Exception e) {
                SharedData.logFirewallEvent("Packet capture error: " + e.getMessage());
            }
        }, "PacketCaptureReader");
        reader.setDaemon(true);
        reader.start();

        // Parser thread
        Thread consumer = new Thread(() -> {
            while (true) {
                try {
                    String pkt = SharedData.packetLines.take();
                    String[] parts = pkt.split("\\s+");
                    if (parts.length >= 3 && "IP".equals(parts[1])) {
                        String srcPort = parts[2];
                        int idx = srcPort.lastIndexOf('.');
                        if (idx > 0) {
                            String srcIp = srcPort.substring(0, idx);
                            SharedData.totalAttempted.incrementAndGet();
                            SharedData.attemptsByIp
                                    .computeIfAbsent(srcIp, k -> new AtomicLong(0))
                                    .incrementAndGet();
                        }
                    }
                } catch (InterruptedException ignored) {}
            }
        }, "PacketCaptureConsumer");
        consumer.setDaemon(true);
        consumer.start();
    }
}
