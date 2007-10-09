/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.integration;

import org.mule.extras.client.MuleClient;
import org.mule.providers.streaming.StreamMessageAdapter;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalStreamingTestComponent;
import org.mule.tck.testmodels.mule.TestStreamingComponent;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.provider.UMOStreamMessageAdapter;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * IMPORTANT - DO NOT RUN THIS TEST IN AN IDE WITH LOG LEVEL OF DEBUG.  USE INFO TO SEE
 * DIAGNOSTICS.  OTHERWISE THE CONSOLE OUTPUT WILL BE SIMILAR SIZE TO DATA TRANSFERRED,
 * CAUSING CONFUSNG AND PROBABLY FATAL MEMORY USE.
 */
public abstract class AbstractStreamingCapacityTestCase extends FunctionalTestCase
{

    public static final long ONE_KB = 1024;
    public static final long ONE_MB = ONE_KB * ONE_KB;
    public static final long ONE_GB = ONE_KB * ONE_MB;
    public static final int MESSAGES = 21;

    protected final Log logger = LogFactory.getLog(getClass());
    private long size;
    private String endpoint;

    public AbstractStreamingCapacityTestCase(long size, String endpoint)
    {
        this.size = size;
        this.endpoint = endpoint;
        setDisposeManagerPerSuite(true);
    }

    public void testSend() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference message = new AtomicReference();

        EventCallback callback = new EventCallback()
        {
            public synchronized void eventReceived(UMOEventContext context, Object component)
            {
                try
                {
                    FunctionalStreamingTestComponent ftc = (FunctionalStreamingTestComponent) component;
                    message.set(ftc.getSummary());
                    latch.countDown();
                }
                catch (Exception e)
                {
                    logger.error(e.getMessage(), e);
                }
            }
        };

        MuleClient client = new MuleClient();

        UMOComponent component = managementContext.getRegistry().lookupComponent("testComponent");
        assertTrue(component instanceof TestStreamingComponent);
        FunctionalStreamingTestComponent ftc = (FunctionalStreamingTestComponent) ((TestStreamingComponent) component).getOrCreateService();
        assertNotNull(ftc);
        //assertEquals(1, ftc.getNumber());

        ftc.setEventCallback(callback, size);

        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // i know, i know...
        long freeStart = runtime.freeMemory();
        long maxStart = runtime.maxMemory();
        long timeStart = System.currentTimeMillis();

        BigInputStream stream = new BigInputStream(size, MESSAGES);
        UMOStreamMessageAdapter adapter = new StreamMessageAdapter(stream);
        client.dispatchStream(endpoint, adapter);

        // if we assume 1MB/sec then we need at least...
        int pause = (int) Math.max(size / ONE_MB, 10);
        logger.info("Waiting for up to " + pause + " seconds");

        latch.await(pause, TimeUnit.SECONDS);
        assertEquals(stream.summary(), message.get());

        // neither of these memory tests are really reliable, but if we stay with 1.4 i don't
        // know of anything better.
        // if these fail in practice i guess we just remove them.

        long freeEnd = runtime.freeMemory();
        long delta = freeStart - freeEnd;
        long timeEnd = System.currentTimeMillis();
        double speed = size / (double) (timeEnd - timeStart) * 1000 / ONE_MB;
        logger.info("Transfer speed " + speed + " MB/s (" + size + " B in " + (timeEnd - timeStart) + " ms)");
        double usePercent = 100.0 * delta / ((double) size);
        logger.info("Memory delta " + delta + " B = " + usePercent + "%");
        assertTrue("Memory used too high", usePercent < 10);

        long maxEnd = runtime.maxMemory();
        assertEquals("Max memory shifted", 0,  maxEnd - maxStart);
    }

}