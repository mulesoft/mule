/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.integration;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalStreamingTestComponent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.model.UMOModel;

import java.util.HashMap;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This test is more about testing the streaming model than the TCP provider, really.
 */
public class StreamingTestCase extends FunctionalTestCase
{

    private static final Log logger = LogFactory.getLog(StreamingTestCase.class);
    public static final int TIMEOUT = 3000;
    public static final String TEST_MESSAGE = "Test TCP Request";
    public static final String RESULT = "Received stream; length: 16; 'Test...uest'";

    public StreamingTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "tcp-streaming-test.xml";
    }

    public void testSend() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference message = new AtomicReference();
        final AtomicInteger loopCount = new AtomicInteger(0);

        EventCallback callback = new EventCallback()
        {
            public synchronized void eventReceived(UMOEventContext context, Object component)
            {
                try
                {
                    logger.info("called " + loopCount.incrementAndGet() + " times");
                    FunctionalStreamingTestComponent ftc = (FunctionalStreamingTestComponent) component;
                    // without this we may have problems with the many repeats
                    if (1 == latch.getCount())
                    {
                        message.set(ftc.getSummary());
                        assertEquals(RESULT, message.get());
                        latch.countDown();
                    }
                }
                catch (Exception e)
                {
                    logger.error(e.getMessage(), e);
                }
            }
        };

        MuleClient client = new MuleClient();

        // this just creates another class - not the one used
//        FunctionalStreamingTestComponent ftc =
//                (FunctionalStreamingTestComponent) MuleManager.getInstance()
//                        .getContainerContext().getComponent(
//                        new ContainerKeyPair("mule", "testComponent"));

        // this creates a new instance too
//        UMOModel model = (UMOModel) MuleManager.getInstance().getModels().get("echo");
//        FunctionalStreamingTestComponent ftc =
//                (FunctionalStreamingTestComponent) model.getComponent("testComponent").getInstance();

        // this works only if singleton set in descriptor
        UMOModel model = managementContext.getRegistry().lookupModel("echoModel");
        FunctionalStreamingTestComponent ftc =
                (FunctionalStreamingTestComponent) model.getComponent("testComponent").getInstance();
        assertNotNull(ftc);
        assertEquals(1, ftc.getNumber());

        // this works with or without singleton, but required adding getComponent method
//        UMOModel model = (UMOModel) MuleManager.getInstance().getModels().get("echo");
//        UMOSession session = model.getComponentSession("testComponent");
//        StreamingComponent component = (StreamingComponent) session.getComponent();
//        FunctionalStreamingTestComponent ftc = (FunctionalStreamingTestComponent) component.getComponent();

        ftc.setEventCallback(callback, TEST_MESSAGE.length());

        client.dispatch("tcp://localhost:65432", TEST_MESSAGE, new HashMap());

        latch.await(10, TimeUnit.SECONDS);
        assertEquals(RESULT, message.get());
    }

}
