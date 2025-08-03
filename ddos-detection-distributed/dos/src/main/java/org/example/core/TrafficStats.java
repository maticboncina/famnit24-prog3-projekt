package org.example.core;

import java.util.ArrayDeque;
import java.util.Deque;

public class TrafficStats {
    private final Deque<Double> window = new ArrayDeque<>();
    private double sum = 0, sumSq = 0;
    private final int max = 60;

    public synchronized void record(double v) {
        window.addLast(v);
        sum   += v;
        sumSq += v * v;
        if (window.size() > max) {
            double old = window.removeFirst();
            sum   -= old;
            sumSq -= old * old;
        }
    }

    public synchronized double mean() {
        return window.isEmpty() ? 0 : sum / window.size();
    }

    public synchronized double stddev() {
        if (window.isEmpty()) return 0;
        double m = mean();
        return Math.sqrt((sumSq / window.size()) - m * m);
    }
}
