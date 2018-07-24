package com.dtstack.jfilebeat.common.ratelimit;

public abstract class Ticker {
	  /**
	   * Constructor for use by subclasses.
	   */
	  protected Ticker() {}

	  /**
	   * Returns the number of nanoseconds elapsed since this ticker's fixed point of reference.
	   */
	  public abstract long read();

	  /**
	   * A ticker that reads the current time using {@link System#nanoTime}.
	   *
	   * @since 10.0
	   */
	  public static Ticker systemTicker() {
	    return SYSTEM_TICKER;
	  }

	  private static final Ticker SYSTEM_TICKER =
	      new Ticker() {
	        @Override
	        public long read() {
	          return System.nanoTime();
	        }
	      };
	}
