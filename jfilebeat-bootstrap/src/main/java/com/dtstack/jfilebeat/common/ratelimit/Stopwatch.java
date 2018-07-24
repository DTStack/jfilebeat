package com.dtstack.jfilebeat.common.ratelimit;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class Stopwatch {
	  private final Ticker ticker;
	  private boolean isRunning;
	  private long elapsedNanos;
	  private long startTick;

	  /**
	   * Creates (but does not start) a new stopwatch using {@link System#nanoTime} as its time source.
	   *
	   * @since 15.0
	   */
	  public static Stopwatch createUnstarted() {
	    return new Stopwatch();
	  }

	  /**
	   * Creates (but does not start) a new stopwatch, using the specified time source.
	   *
	   * @since 15.0
	   */
	  public static Stopwatch createUnstarted(Ticker ticker) {
	    return new Stopwatch(ticker);
	  }

	  /**
	   * Creates (and starts) a new stopwatch using {@link System#nanoTime} as its time source.
	   *
	   * @since 15.0
	   */
	  public static Stopwatch createStarted() {
	    return new Stopwatch().start();
	  }

	  /**
	   * Creates (and starts) a new stopwatch, using the specified time source.
	   *
	   * @since 15.0
	   */
	  public static Stopwatch createStarted(Ticker ticker) {
	    return new Stopwatch(ticker).start();
	  }

	  Stopwatch() {
	    this.ticker = Ticker.systemTicker();
	  }

	  Stopwatch(Ticker ticker) {
	    this.ticker = ticker;
	  }

	  /**
	   * Returns {@code true} if {@link #start()} has been called on this stopwatch, and {@link #stop()}
	   * has not been called since the last call to {@code start()}.
	   */
	  public boolean isRunning() {
	    return isRunning;
	  }

	  /**
	   * Starts the stopwatch.
	   *
	   * @return this {@code Stopwatch} instance
	   * @throws IllegalStateException if the stopwatch is already running.
	   */
	  public Stopwatch start() {
	    isRunning = true;
	    startTick = ticker.read();
	    return this;
	  }

	  /**
	   * Stops the stopwatch. Future reads will return the fixed duration that had elapsed up to this
	   * point.
	   *
	   * @return this {@code Stopwatch} instance
	   * @throws IllegalStateException if the stopwatch is already stopped.
	   */
	  public Stopwatch stop() {
	    long tick = ticker.read();
	    isRunning = false;
	    elapsedNanos += tick - startTick;
	    return this;
	  }

	  /**
	   * Sets the elapsed time for this stopwatch to zero, and places it in a stopped state.
	   *
	   * @return this {@code Stopwatch} instance
	   */
	  public Stopwatch reset() {
	    elapsedNanos = 0;
	    isRunning = false;
	    return this;
	  }

	  private long elapsedNanos() {
	    return isRunning ? ticker.read() - startTick + elapsedNanos : elapsedNanos;
	  }

	  /**
	   * Returns the current elapsed time shown on this stopwatch, expressed in the desired time unit,
	   * with any fraction rounded down.
	   *
	   * <p><b>Note:</b> the overhead of measurement can be more than a microsecond, so it is generally
	   * not useful to specify {@link TimeUnit#NANOSECONDS} precision here.
	   *
	   * <p>It is generally not a good idea to use an ambiguous, unitless {@code long} to represent
	   * elapsed time. Therefore, we recommend using {@link #elapsed()} instead, which returns a
	   * strongly-typed {@link Duration} instance.
	   *
	   * @since 14.0 (since 10.0 as {@code elapsedTime()})
	   */
	  public long elapsed(TimeUnit desiredUnit) {
	    return desiredUnit.convert(elapsedNanos(), NANOSECONDS);
	  }

	  /**
	   * Returns the current elapsed time shown on this stopwatch as a {@link Duration}. Unlike {@link
	   * #elapsed(TimeUnit)}, this method does not lose any precision due to rounding.
	   *
	   * @since 22.0
	   */
	  public Duration elapsed() {
	    return Duration.ofNanos(elapsedNanos());
	  }

	  private static TimeUnit chooseUnit(long nanos) {
	    if (DAYS.convert(nanos, NANOSECONDS) > 0) {
	      return DAYS;
	    }
	    if (HOURS.convert(nanos, NANOSECONDS) > 0) {
	      return HOURS;
	    }
	    if (MINUTES.convert(nanos, NANOSECONDS) > 0) {
	      return MINUTES;
	    }
	    if (SECONDS.convert(nanos, NANOSECONDS) > 0) {
	      return SECONDS;
	    }
	    if (MILLISECONDS.convert(nanos, NANOSECONDS) > 0) {
	      return MILLISECONDS;
	    }
	    if (MICROSECONDS.convert(nanos, NANOSECONDS) > 0) {
	      return MICROSECONDS;
	    }
	    return NANOSECONDS;
	  }

	  private static String abbreviate(TimeUnit unit) {
	    switch (unit) {
	      case NANOSECONDS:
	        return "ns";
	      case MICROSECONDS:
	        return "\u03bcs"; // μs
	      case MILLISECONDS:
	        return "ms";
	      case SECONDS:
	        return "s";
	      case MINUTES:
	        return "min";
	      case HOURS:
	        return "h";
	      case DAYS:
	        return "d";
	      default:
	        throw new AssertionError();
	    }
	  }
	}
