/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.timer;

import org.mule.tck.AbstractMuleTestCase;

import java.util.Timer;

public class TimerTestCase extends AbstractMuleTestCase implements TimeEventListener
{
    volatile boolean fired;

    public void testTimer() throws Exception
    {
        Timer timer = new Timer();

        EventTimerTask task = new EventTimerTask(this);

        timer.schedule(task, 0, 1000);
        task.start();
        Thread.sleep(1500);
        assertTrue(fired);
    }

    public void testStopTimer() throws Exception
    {
        fired = false;
        Timer timer = new Timer();

        EventTimerTask task = new EventTimerTask(this);

        timer.schedule(task, 0, 1000);
        task.start();
        Thread.sleep(1500);
        assertTrue(fired);
        fired = false;
        task.stop();
        Thread.sleep(1500);
        assertTrue(!fired);
    }

    public void testMultipleListeners() throws Exception
    {
        fired = false;
        Timer timer = new Timer();
        AnotherListener listener = new AnotherListener();

        EventTimerTask task = new EventTimerTask(this);
        task.addListener(listener);

        timer.schedule(task, 0, 1000);

        task.start();
        Thread.sleep(1500);
        assertTrue(fired);
        assertTrue(listener.wasFired());
        listener.setWasFired(false);

        fired = false;
        task.stop();
        Thread.sleep(1500);
        assertTrue(!fired);
        assertTrue(!listener.wasFired());
    }

    public void testRemoveListeners() throws Exception
    {
        fired = false;
        Timer timer = new Timer();
        AnotherListener listener = new AnotherListener();

        EventTimerTask task = new EventTimerTask(this);
        task.addListener(listener);

        timer.schedule(task, 0, 1000);

        task.start();
        Thread.sleep(1500);
        assertTrue(fired);
        assertTrue(listener.wasFired());
        listener.setWasFired(false);

        fired = false;
        task.stop();
        task.removeListener(this);
        task.start();
        Thread.sleep(1500);
        assertTrue(!fired);
        assertTrue(listener.wasFired());
        listener.setWasFired(false);
        task.stop();
        task.removeAllListeners();
        task.start();
        Thread.sleep(1500);
        assertTrue(!fired);
        assertTrue(!listener.wasFired());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.util.timer.TimeEventListener#timeExpired(org.mule.util.timer.TimeEvent)
     */
    public void timeExpired(TimeEvent e)
    {
        assertTrue(e.getTimeExpired() > 0);
        assertNotNull(e.getName());
        fired = true;

    }

    private class AnotherListener implements TimeEventListener
    {

        private boolean wasFired;

        /*
         * (non-Javadoc)
         * 
         * @see org.mule.util.timer.TimeEventListener#timeExpired(org.mule.util.timer.TimeEvent)
         */
        public void timeExpired(TimeEvent e)
        {
            wasFired = true;

        }

        /**
         * @return Returns the wasFired.
         */
        public boolean wasFired()
        {
            return wasFired;
        }

        /**
         * @param wasFired The wasFired to set.
         */
        public void setWasFired(boolean wasFired)
        {
            this.wasFired = wasFired;
        }

    }

}
