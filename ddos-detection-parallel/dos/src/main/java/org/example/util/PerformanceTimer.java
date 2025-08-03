package org.example.util;

/**
 * Simple high-resolution timer for benchmarking code sections.
 */
public class PerformanceTimer {
    private long startTime;
    private long endTime;

    /** Starts (or restarts) the timer. */
    public void start() {
        startTime = System.nanoTime();
    }

    /** Stops the timer. */
    public void stop() {
        endTime = System.nanoTime();
    }

    /**
     * @return Elapsed time between start() and stop(), in milliseconds.
     *         If stop() hasn’t been called, returns time since start().
     */
    public double getElapsedMillis() {
        long now = (endTime != 0 ? endTime : System.nanoTime());
        return (now - startTime) / 1_000_000.0;
    }

    /**
     * Convenience: starts the timer, runs the given Runnable, then stops.
     * @return Elapsed milliseconds.
     */
    public static double time(Runnable task) {
        PerformanceTimer t = new PerformanceTimer();
        t.start();
        task.run();
        t.stop();
        return t.getElapsedMillis();
    }
}
