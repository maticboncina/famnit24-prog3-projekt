package org.example.ui;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import org.example.core.SharedData;
import org.example.core.TrafficStats;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

public class MonitoringPanel extends JPanel {
    private final JLabel statusLabel, inboundLabel, servedLabel, topIpLabel, blockedLabel;
    private final JLabel hardLabel,   minLabel;
    private final JSlider hardSlider, minSlider;
    private final TimeSeries inSeries, outSeries;
    private final Map<String,Integer> belowCount = new ConcurrentHashMap<>();
    private final Map<String,Long>    lastByIp   = new ConcurrentHashMap<>();
    private long lastTotalAtt = 0, lastTotalServ = 0;
    private static final int COOLDOWN = 3;
    private final ScheduledExecutorService sched = Executors.newSingleThreadScheduledExecutor();

    public MonitoringPanel() {
        setLayout(new BorderLayout(10,10));

        // Limits panel
        JPanel limits = new JPanel(new GridLayout(2,2,5,5));
        hardLabel  = new JLabel("Hard-limit: " + SharedData.hardLimit);
        hardSlider = new JSlider(1,1000,SharedData.hardLimit);
        hardSlider.addChangeListener(e -> {
            SharedData.hardLimit = hardSlider.getValue();
            hardLabel.setText("Hard-limit: " + SharedData.hardLimit);
        });
        minLabel  = new JLabel("Min-limit: " + SharedData.minLimit);
        minSlider = new JSlider(0,500,SharedData.minLimit);
        minSlider.addChangeListener(e -> {
            SharedData.minLimit = minSlider.getValue();
            minLabel.setText("Min-limit: " + SharedData.minLimit);
        });
        limits.add(hardLabel);
        limits.add(hardSlider);
        limits.add(minLabel);
        limits.add(minSlider);

        // Info panel
        JPanel info = new JPanel(new GridLayout(2,3,5,5));
        statusLabel   = new JLabel("Status: Normal");
        inboundLabel  = new JLabel("Inbound: 0 req/s");
        servedLabel   = new JLabel("Served:  0 req/s");
        topIpLabel    = new JLabel("Top Src: None");
        blockedLabel  = new JLabel("Blocked: None");
        info.add(statusLabel);
        info.add(inboundLabel);
        info.add(servedLabel);
        info.add(topIpLabel);
        info.add(blockedLabel);

        JPanel north = new JPanel(new BorderLayout(5,5));
        north.add(limits, BorderLayout.NORTH);
        north.add(info,   BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        inSeries  = new TimeSeries("Inbound");
        outSeries = new TimeSeries("Served");
        ChartPanel inChart = new ChartPanel(
                ChartFactory.createTimeSeriesChart(
                        "Inbound Traffic","Time","Req/s",
                        new TimeSeriesCollection(inSeries),
                        false,true,false)
        );
        ChartPanel outChart = new ChartPanel(
                ChartFactory.createTimeSeriesChart(
                        "Served Traffic","Time","Req/s",
                        new TimeSeriesCollection(outSeries),
                        false,true,false)
        );
        JPanel charts = new JPanel(new GridLayout(2,1));
        charts.add(inChart);
        charts.add(outChart);
        add(charts, BorderLayout.CENTER);

        sched.scheduleAtFixedRate(this::updateMetrics, 1, 1, TimeUnit.SECONDS);
    }

    private void updateMetrics() {
        long currAtt = SharedData.totalAttempted.get();
        long deltaAtt = currAtt - lastTotalAtt; lastTotalAtt = currAtt;
        long currServ = SharedData.totalServed.get();
        long deltaServ = currServ - lastTotalServ; lastTotalServ = currServ;

        String topIp = null; long max = 0;
        Set<String> ips = new HashSet<>(SharedData.attemptsByIp.keySet());
        for (String ip: ips) {
            long tot  = SharedData.attemptsByIp.get(ip).get();
            long prev = lastByIp.getOrDefault(ip, 0L);
            long delta= tot - prev; lastByIp.put(ip, tot);
            if (delta > max) { max = delta; topIp = ip; }

            TrafficStats stats = SharedData.statsByIp.computeIfAbsent(ip, k->new TrafficStats());
            double mean = stats.mean();
            double sd   = stats.stddev();
            double upper = mean + 2*sd;
            boolean aboveMin = delta >= SharedData.minLimit;
            boolean blockCond = aboveMin && (delta > SharedData.hardLimit || delta > upper);
            boolean clearCond = delta <= SharedData.hardLimit && delta <= upper;

            if (!SharedData.blockedIPs.contains(ip) && blockCond) {
                SharedData.blockedIPs.add(ip);
                belowCount.remove(ip);
                SharedData.logFirewallEvent("Blocked " + ip + " (rps="+delta+")");
            } else if (SharedData.blockedIPs.contains(ip)) {
                int cnt = clearCond ? belowCount.getOrDefault(ip,0)+1 : 0;
                if (cnt >= COOLDOWN) {
                    SharedData.blockedIPs.remove(ip);
                    belowCount.remove(ip);
                    SharedData.logFirewallEvent("Unblocked " + ip);
                } else {
                    belowCount.put(ip, cnt);
                }
            }
            stats.record(delta);
        }

        boolean attack = !SharedData.blockedIPs.isEmpty();
        String finalTopIp = topIp;
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Status: " + (attack ? "Under Attack" : "Normal"));
            inboundLabel.setText(String.format("Inbound: %d req/s", deltaAtt));
            servedLabel .setText(String.format("Served:  %d req/s", deltaServ));
            topIpLabel  .setText("Top Src: " + (finalTopIp != null ? finalTopIp : "None"));
            blockedLabel.setText("Blocked: " +
                    (SharedData.blockedIPs.isEmpty() ? "None" : String.join(",", SharedData.blockedIPs))
            );
            inSeries .addOrUpdate(new Millisecond(), deltaAtt);
            outSeries.addOrUpdate(new Millisecond(), deltaServ);
        });
    }
}
