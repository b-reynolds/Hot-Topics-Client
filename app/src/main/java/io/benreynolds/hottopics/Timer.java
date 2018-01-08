package io.benreynolds.hottopics;

/**
 * {@code Timer} provides a simple set of timing functionality that allows users to measure elapsed time in seconds.
 * Time is measured using the Java Virtual Machine's high-resolution time source ({@code System.nanoTime()}).
 */
class Timer {

    /** Default interval value of the {@code Timer}. The default value of 0.0 ensures that {@code hasElapsed()} will
     *  return {@code true} until the '{@code Timer}'s interval is set. */
    private static final double DEFAULT_INTERVAL = 0.0;

    /** Amount of time that has to pass after {@code start()} is called before {@code hasElapsed()} will return
     * {@code true}. */
    private double mInterval;

    /** Used to store the time at which the {@code Timer} was started (see {@code start()}). This value is used in
     *  conjunction with {@code mInterval} to determine whether the timer has elapsed or not when {@code hasElapsed()}
     *  is called. */
    private long mStartTime;

    /**
     * Instantiates the {@code Timer} using the default interval time (see {@code DEFAULT_INTERVAL}).
     */
    Timer() {
        this(DEFAULT_INTERVAL);
    }

    /**
     * Instantiates the {@code Timer} using the specified interval time.
     * @param interval interval time (in seconds).
     */
    Timer(final double interval) {
        mInterval = interval;
        mStartTime = System.nanoTime();
    }

    /**
     * Sets the '{@code Timer}'s interval.
     * @param interval interval time (in seconds).
     */
    void setInterval(double interval) {
        mInterval = Math.abs(interval);
    }

    /**
     * Returns the '{@code Timer}'s interval.
     * @return '{@code Timer}'s interval.
     */
    double getInterval() {
        return mInterval;
    }

    /**
     * Returns the amount of time remaining before the {@code Timer} elapses (in seconds).
     * @return Amount of time remaining before the {@code Timer} elapses (in seconds).
     */
    double getTimeRemaining() {
        double timeRemaining = mInterval - (convertNanoSecondsToSeconds(System.nanoTime()) - convertNanoSecondsToSeconds(mStartTime));
        return timeRemaining > 0.0 ? timeRemaining : 0;
    }

    /**
     * Returns {@code true} if the '{@code Timer}'s interval has elapsed.
     * @return {@code true} if the '{@code Timer}'s interval has elapsed.
     */
    boolean hasElapsed() {
        return getTimeRemaining() == 0.0;
    }

    /**
     * Starts (or resets) the '{@code Timer}' (using the specified interval).
     */
    void start() {
        mStartTime = System.nanoTime();
    }

    /**
     * Converts a nanosecond value into seconds.
     * @param nanoSeconds Nanosecond value.
     * @return Nanosecond value converted into seconds.
     */
    private double convertNanoSecondsToSeconds(long nanoSeconds) {
        return ((double)nanoSeconds) / 1E9;
    }

}
