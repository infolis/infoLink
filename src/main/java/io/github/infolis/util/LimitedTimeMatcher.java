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
	
	private static Logger log = LoggerFactory.getLogger(LimitedTimeMatcher.class);
	
	private static final long SLEEP_TIME_MILLIS = 100;

    private boolean matched;
	private boolean finished;
	private double timePassedMillis = 0;

	private long maxTime;

	private long startTime;
	private Matcher matcher;

	protected String threadName;
	private final int lastPos = 0;
	

    public LimitedTimeMatcher(final Pattern pattern, final String str, long maxTimeMillis, final String threadName) {
    	
    	this.startTime = System.nanoTime();
    	this.maxTime = maxTimeMillis;
    	this.matcher = pattern.matcher(new InterruptibleCharSequence(str.substring(lastPos)));
    	this.threadName = threadName;
    }

    @Override
	public void run() {
		Thread matcherThread = new Thread(new Runnable() {
			public void run() {
				// NOTE this is the blocking call
				matched = matcher.find();
				finished = true;
			}
		}, threadName);
		matcherThread.start();
		while (true) {
			timePassedMillis += (System.nanoTime() - startTime) / 1_000_000.0;
			try {
				Thread.sleep(SLEEP_TIME_MILLIS);
			} catch (InterruptedException e) {
				log.error("Logging thread was interrupted.");
				break;
			}
			if (finished) {
				log.debug("Thread '{}' took {} ms to finish", threadName, timePassedMillis);
				break;
			} else if (timePassedMillis > maxTime) {
				log.warn("Thread '{}' took longer than {} ms, shutting down to avoid pathological backtacking.", threadName, maxTime);
				log.debug("{}", timePassedMillis);
				matcherThread.interrupt();
				break;
			}
			log.debug("Thread '{}' running for {}ms", threadName, timePassedMillis);
		}
	}

    /**
     * @return whether the matcher succeeded in finding or not
     */
    public boolean matched() {
        return matched;
    }
    
    public boolean finished() {
    	return finished;
    }
    
    public Matcher getMatcher() {
		return matcher;
	}
    
    /**
     * CharSequence that noticed thread interrupts -- as might be necessary 
     * to recover from a loose regex on unexpected challenging input. 
     * 
     * @see {@linkplain http://stackoverflow.com/questions/910740/cancelling-a-long-running-regex-match}
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
}
