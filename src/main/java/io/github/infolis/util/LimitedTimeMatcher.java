/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runnable implementation of the matcher.find() method for handling catastropic
 * backtracking. May be passed to a thread to be monitored and cancelled in case
 * catastrophic backtracking occurs while searching for a regular expression.
 *
 * @author kba
 * @author katarina.boland@gesis.org
 * @author gojomo
 *
 */
public class LimitedTimeMatcher implements Runnable {

	/**
	 * CharSequence that noticed thread interrupts -- as might be necessary to
	 * recover from a loose regex on unexpected challenging input.
	 * 
	 * @see {@linkplain http
	 *      ://stackoverflow.com/questions/910740/cancelling-a-long
	 *      -running-regex-match}
	 * @author gojomo
	 */
	public class InterruptibleCharSequence implements CharSequence {
		CharSequence inner;

		public InterruptibleCharSequence(CharSequence inner) {
			super();
			this.inner = inner;
		}

		public char charAt(int index) {
			if (Thread.interrupted()) { // clears flag if set
				throw new RuntimeException(new InterruptedException());
			}
			// counter++;
			return inner.charAt(index);
		}

		public int length() {
			return inner.length();
		}

		public CharSequence subSequence(int start, int end) {
			return new InterruptibleCharSequence(inner.subSequence(start, end));
		}

		@Override
		public String toString() {
			return inner.toString();
		}
	}

	private static Logger log = LoggerFactory.getLogger(LimitedTimeMatcher.class);

	private static final long SLEEP_TIME_MILLIS = 10;

	private boolean matched;
	private boolean finished;
	private boolean timedOut;
	private double timePassedMillis;
	private final long maxTime;
	private final long startTime;
	private final Matcher matcher;

	protected String threadName;
	private int lastMatchEnd = 0;

	/**
	 * @param pattern
	 *            {@link Pattern} to run
	 * @param str
	 *            {@link String} to match
	 * @param maxTimeMillis
	 *            maximum number of milliseconds before stopping the matching
	 *            thread
	 * @param threadName
	 *            arbitrary name of this thread for mnemonics
	 */
	public LimitedTimeMatcher(final Pattern pattern, final String str, long maxTimeMillis,
			final String threadName) {
		this.startTime = System.nanoTime();
		this.maxTime = maxTimeMillis;
		this.matcher = pattern.matcher(new InterruptibleCharSequence(str.substring(end())));
		this.threadName = threadName;
	}

	@Override
	public void run() {
		Thread matcherThread = new Thread(new Runnable() {
			// NOTE this is the blocking call
			public void run() {
				if (timedOut()) {
					log.error("Calling run() on a LimitedTimeMatcher that timed out before!");
					return;
				}
				finished(false);
				matched(getMatcher().find());
				end(matched() ? matcher.end() : 0);
				finished(true);
			}
		}, threadName);
		setTimePassedMillis(0);
		matcherThread.start();
		while (true) {
			setTimePassedMillis(getTimePassedMillis() + (System.nanoTime() - startTime) / 1_000_000.0);
			try {
				Thread.sleep(SLEEP_TIME_MILLIS);
			} catch (InterruptedException e) {
				log.error("Logging thread was interrupted.");
				break;
			}
			if (finished()) {
				log.trace("Thread '{}' took {} ms to finish", threadName, getTimePassedMillis());
				break;
			} else if (timePassedMillis > maxTime) {
				timedOut(true);
				log.warn("Thread '{}' took {} ms, longer than the maximum of {} ms, shutting down to avoid pathological backtacking.",
						threadName, getTimePassedMillis(), maxTime);
				matcherThread.interrupt();
				break;
			}
			log.trace("Thread '{}' running for {}ms", threadName, getTimePassedMillis());
		}
	}

	private Matcher getMatcher() {
		return matcher;
	}

	private synchronized void setTimePassedMillis(double timePassedMillis) {
		this.timePassedMillis = timePassedMillis;
	}

	/**
	 * @return the number of ms the last invocation ran for.
	 */
	public synchronized double getTimePassedMillis() {
		return timePassedMillis;
	}

	/**
	 * @return Whether the last run of the matcher timed out
	 */
	synchronized boolean timedOut() {
		return timedOut;
	}

	private synchronized void timedOut(boolean timedOut) {
		this.timedOut = timedOut;
	}

	/**
	 * @return Whether the last run of the matcher finished successfully
	 */
	public synchronized boolean finished() {
		return finished;
	}

	private synchronized void finished(boolean finished) {
		this.finished = finished;
	}

	/**
	 * @return whether the last run of the matcher matched
	 */
	public synchronized boolean matched() {
		return matched;
	}

	private synchronized void matched(boolean matched) {
		this.matched = matched;
	}

	/**
	 * @return the end of the last match
	 */
	public synchronized int end() {
		return lastMatchEnd;
	}

	private synchronized void end(int lastPos) {
		this.lastMatchEnd = lastPos;
	}

	/**
	 * Returns the input subsequence captured by the given group during the
	 * previous match operation.
	 * 
	 * @see Matcher#group(int)
	 */
	public String group(int i) {
		return getMatcher().group(i);
	}

	/**
	 * Returns the input subsequence matched by the previous match.
	 * 
	 * @see Matcher#group()
	 */
	public String group() {
		// TODO Auto-generated method stub
		return getMatcher().group();
	}

}
