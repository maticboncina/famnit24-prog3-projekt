package org.example.core;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.ui.FirewallLogPanel;

import java.io.OutputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class SharedData {
    public static final BlockingQueue<String> packetLines   = new LinkedBlockingQueue<>();
    public static Process                       tcpdumpProcess;

    public static final AtomicLong                                 totalAttempted = new AtomicLong(0);
    public static final AtomicLong                                 totalServed    = new AtomicLong(0);
    public static final ConcurrentHashMap<String, AtomicLong>      attemptsByIp   = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, AtomicLong>      servedByIp     = new ConcurrentHashMap<>();

    public static final Set<String>                                blockedIPs     = ConcurrentHashMap.newKeySet();
    public static final ConcurrentHashMap<String, TrafficStats>    statsByIp      = new ConcurrentHashMap<>();

    public static final ExecutorService                            httpExecutor   = Executors.newFixedThreadPool(8);

    public static volatile int hardLimit = 369;
    public static volatile int minLimit  = 1;

    public static FirewallLogPanel firewallLogPanel;
    public static final DateTimeFormatter LOG_TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void logFirewallEvent(String event) {
        String ts   = LocalDateTime.now().format(LOG_TS_FMT);
        String line = String.format("[%s] %s%n", ts, event);
        if (firewallLogPanel != null) firewallLogPanel.appendLog(line);
    }

    public static final HttpHandler handler = exchange -> {
        String ip = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        }

        totalAttempted.incrementAndGet();
        attemptsByIp
                .computeIfAbsent(ip, k -> new AtomicLong(0))
                .incrementAndGet();

        if (blockedIPs.contains(ip)) {
            exchange.sendResponseHeaders(403, -1);
            exchange.close();
            return;
        }

        totalServed.incrementAndGet();
        servedByIp
                .computeIfAbsent(ip, k -> new AtomicLong(0))
                .incrementAndGet();

        URI uri = exchange.getRequestURI();
        if ("/".equals(uri.getPath())) {
            exchange.getResponseHeaders().add("Location", "/index.html");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
            return;
        }

        String resp = "<html><body><h1>Welcome to DoS Test Server</h1></body></html>";
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        byte[] bs = resp.getBytes();
        exchange.sendResponseHeaders(200, bs.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bs);
        }
    };
}
