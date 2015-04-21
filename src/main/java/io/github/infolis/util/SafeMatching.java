/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.infolis.util;

import java.util.regex.Matcher;

/**
 * Runnable implementation of the matcher.find() method for handling catastropic
 * backtracking. May be passed to a thread to be monitored and cancelled in case
 * catastrophic backtracking occurs while searching for a regular expression.
 *
 * @author katarina.boland@gesis.org
 *
 */
public class SafeMatching implements Runnable {

    Matcher matcher;
    private boolean find;

    /**
     * Class constructor initializing the matcher.
     *
     * @param m	the Matcher instance to be used for matching
     */
    public SafeMatching(Matcher m) {
        this.matcher = m;
    }

    @Override
    public void run() {
        this.setFind(this.matcher.find());
    }

    /**
     * Monitors the given thread and stops it when it exceeds its time-to-live.
     * Calls itself until the thread ends after completing its task or after
     * being stopped.
     *
     * @param thread	the thread to be monitored
     * @param maxProcessTimeMillis	the maximum time-to-live for thread
     * @param startTimeMillis	thread's birthday :)
     * @return	false, if thread was stopped prematurely; true if thread ended
     * after completion of its task
     */
    @SuppressWarnings("deprecation")
    public boolean threadCompleted(Thread thread, long maxProcessTimeMillis, long startTimeMillis) {
        if (thread.isAlive()) {
            long curProcessTime = System.currentTimeMillis() - startTimeMillis;
            System.out.println("Thread " + thread.getName() + " running for " + curProcessTime + " millis.");
            if (curProcessTime > maxProcessTimeMillis) {
                System.out.println("Thread taking too long, aborting (" + thread.getName());
                thread.stop();
                return false;
            }
        } else {
            return true;
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {;
        }
        return threadCompleted(thread, maxProcessTimeMillis, startTimeMillis);
    }

    /**
     * @return the find
     */
    public boolean isFind() {
        return find;
    }

    /**
     * @param find the find to set
     */
    public void setFind(boolean find) {
        this.find = find;
    }
}
