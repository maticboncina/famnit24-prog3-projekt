package org.example.ui;

import javax.swing.*;
import java.awt.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class AttackSimulatorPanel extends JPanel {
    private static final String[] CLIENT_IPS = {"127.0.0.1","127.0.0.2","127.0.0.3"};
    private final JTextField urlField;
    private final ClientConfigPanel[] clientPanels;
    private final JButton startBtn, stopBtn;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(CLIENT_IPS.length);
    private final List<ScheduledFuture<?>> tasks = new ArrayList<>();

    public AttackSimulatorPanel() {
        setLayout(new BorderLayout(10,10));

        // URL input
        JPanel top = new JPanel(new BorderLayout(5,5));
        top.add(new JLabel("Target URL:"), BorderLayout.WEST);
        urlField = new JTextField("http://127.0.0.1:8080/index.html");
        top.add(urlField, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // Client panels
        JPanel center = new JPanel(new GridLayout(CLIENT_IPS.length,1,5,5));
        clientPanels = new ClientConfigPanel[CLIENT_IPS.length];
        for (int i=0; i<CLIENT_IPS.length; i++) {
            clientPanels[i] = new ClientConfigPanel(CLIENT_IPS[i]);
            center.add(clientPanels[i]);
        }
        add(center, BorderLayout.CENTER);

        // Controls
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startBtn = new JButton("Start Simulation");
        stopBtn  = new JButton("Stop Simulation");
        stopBtn.setEnabled(false);
        bottom.add(startBtn);
        bottom.add(stopBtn);
        add(bottom, BorderLayout.SOUTH);

        startBtn.addActionListener(e -> {
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            for (ClientConfigPanel cp: clientPanels) {
                ScheduledFuture<?> f = scheduler.scheduleAtFixedRate(
                        () -> simulate(cp), 0, 200, TimeUnit.MILLISECONDS);
                tasks.add(f);
            }
        });
        stopBtn.addActionListener(e -> {
            tasks.forEach(t -> t.cancel(true));
            tasks.clear();
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
        });
    }

    private void simulate(ClientConfigPanel cp) {
        if (!cp.isEnabled()) return;
        int rps = cp.getSpeed();
        int cnt = (int)Math.round(rps * 0.2);
        for (int i=0; i<cnt; i++) sendRequest(cp.getIp());
    }

    private void sendRequest(String ip) {
        try {
            String raw = urlField.getText().trim();
            if (!raw.startsWith("http")) raw = "http://"+raw;
            URL url = new URL(raw);
            HttpURLConnection c = (HttpURLConnection)url.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("X-Forwarded-For", ip);
            c.setConnectTimeout(1000);
            c.setReadTimeout(1000);
            c.getResponseCode();
            c.disconnect();
        } catch (Exception ignored) {}
    }
}
