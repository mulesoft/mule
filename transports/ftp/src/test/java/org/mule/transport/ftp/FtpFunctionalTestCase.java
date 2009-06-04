/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ftp;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalStreamingTestComponent;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transport.file.FileConnector;

import java.util.HashMap;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;

public class FtpFunctionalTestCase extends AbstractFtpServerTestCase
{
    private static int PORT = 60198;

    public FtpFunctionalTestCase()
    {
        super(PORT);
    }

    protected String getConfigResources()
    {
        return "ftp-functional-test.xml";
    }

    protected int getPort()
    {
        return PORT;
    }
        
    public void testSendAndRequest() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference message = new AtomicReference();
        final AtomicInteger loopCount = new AtomicInteger(0);

        EventCallback callback = new EventCallback()
        {
            public synchronized void eventReceived(MuleEventContext context, Object component)
            {
                try
                {
                    logger.info("called " + loopCount.incrementAndGet() + " times");
                    FunctionalTestComponent ftc = (FunctionalTestComponent) component;
                    // without this we may have problems with the many repeats
                    if (1 == latch.getCount())
                    {
                        String o = new String((byte[])ftc.getLastReceivedMessage());
                        message.set(o);
                        latch.countDown();
                    }
                }
                catch (Exception e)
                {
                    logger.error(e.getMessage(), e);
                }
            }
        };
        
        Map properties = new HashMap();
        MuleClient client = new MuleClient();
        assertTrue(getFtpClient().expectFileCount("/", 0, 1000));
        
        Object component = getComponent("testComponent");
        assertTrue("FunctionalTestComponent expected", component instanceof FunctionalTestComponent);
        FunctionalTestComponent ftc = (FunctionalTestComponent) component;
        assertNotNull(ftc);
        
        ftc.setEventCallback(callback);
        
        logger.debug("before dispatch");
        client.dispatch(getMuleFtpEndpoint(), TEST_MESSAGE, properties);
        //client.send(getMuleFtpEndpoint(), TEST_MESSAGE, properties);
        logger.debug("before retrieve");
        
        // TODO DZ: need a reliable way to check the file once it's been written to
        // the ftp server. Currently, once mule processes the ftp'd file, it
        // auto-deletes it, so we can't check it
        //assertTrue(getFtpClient().expectFileCount("/", 1, 10000));
        
        latch.await(getTimeout(), TimeUnit.MILLISECONDS);
        assertEquals(TEST_MESSAGE, message.get());                
    }

}
