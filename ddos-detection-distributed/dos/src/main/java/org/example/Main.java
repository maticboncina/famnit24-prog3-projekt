package org.example;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.example.core.PacketCapture;
import org.example.core.PacketConsumer;
import org.example.core.HttpServerStarter;
import org.example.core.SharedData;
import org.example.ui.AttackSimulatorPanel;
import org.example.ui.MonitoringPanel;
import org.example.ui.FirewallLogPanel;

public class Main {
    public static void main(String[] args) throws Exception {
        PacketCapture.startPacketCapture();
        PacketConsumer.startPacketConsumer();
        HttpServerStarter.startHttpServer();

        SwingUtilities.invokeLater(() -> {
            AttackSimulatorPanel simulator = new AttackSimulatorPanel();
            MonitoringPanel     monitor   = new MonitoringPanel();
            FirewallLogPanel    logs      = new FirewallLogPanel();

            // Wire up the log panel so SharedData.logFirewallEvent() works
            SharedData.firewallLogPanel = logs;

            JFrame frame = new JFrame("DoS Detection Dashboard");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 750);

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Simulator", simulator);
            tabs.addTab("Monitor",   monitor);
            tabs.addTab("Logs",      logs);

            frame.add(tabs);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
