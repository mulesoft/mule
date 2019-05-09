/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Before;
import org.junit.Test;

public class DeferredCloserTestCase
{

    BlockingQueue queue = new LinkedBlockingQueue();
    private Thread printerThread;

    @Before
    public void setupThread() {
        printerThread = new PrintFromQueueThread();
    }

    @Test(timeout = 10000)
    public void restartThread() throws InterruptedException
    {
        printerThread.start();

        // Print sth, wait and again
        queue.put("Hola 1");
        silentSleep(1000);
        queue.put("Hola 2");
        silentSleep(1000);

        // interrupt thread
        printerThread.interrupt();
        silentSleep(1000);
        // Add two thing in queue
        queue.put("Hola denuevo 1");
        queue.put("Hola denuevo 2");
        printerThread = new PrintFromQueueThread();
        printerThread.start();

        probeGetsEmptied();
    }

    @Test(timeout = 10000)
    public void closingTest() throws InterruptedException
    {

        queue.put("COMIENZO");

        printerThread.start();

        silentSleep(2000);
        queue.put("Hola perro");


        silentSleep(2000);
        queue.put("Chau perro");

        probeGetsEmptied();
    }

    private void probeGetsEmptied()
    {
        new PollingProber(10000, 1000).check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return queue.isEmpty();
            }

            @Override
            public String describeFailure()
            {
                return "Queue should have been empty";
            }
        });
    }

    private class PrintFromQueueThread extends Thread
    {

        PrintFromQueueThread()  {
            super("PrinterThread");
        }

        @Override
        public void run()
        {
            while (!Thread.currentThread().isInterrupted())
            {
                // Think this will block waiting
                Object closable;
                try
                {
                    closable = queue.take();
                    System.out.println(String.format("Just removed this element: %s", closable.toString()));
                }
                catch (InterruptedException e)
                {
                    System.out.println("Just got interrupted waiting for new elements");
                }
            }
            System.out.println("Just got interrupted");
        }
    }

    private void silentSleep(Integer millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            System.out.println("Main thread interrupted");
        }
    }

}
