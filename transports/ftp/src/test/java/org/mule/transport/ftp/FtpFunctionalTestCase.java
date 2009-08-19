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
import org.mule.module.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
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

    /**
     * Used by subclasses (in EE)
     */
    public int getPort()
    {
        return PORT;
    }
    
    public void testSendAndRequest() throws Exception
    {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference message = new AtomicReference();

        Object component = getComponent("testComponent");
        assertNotNull(component);
        assertTrue("FunctionalTestComponent expected", component instanceof FunctionalTestComponent);
        FunctionalTestComponent ftc = (FunctionalTestComponent) component;
        ftc.setEventCallback(new FunctionalEventCallback(latch, message));
        
        MuleClient client = new MuleClient();
        client.dispatch(getMuleFtpEndpoint(), TEST_MESSAGE, null);
        
        // TODO DZ: need a reliable way to check the file once it's been written to
        // the ftp server. Currently, once mule processes the ftp'd file, it
        // auto-deletes it, so we can't check it
        //assertTrue(getFtpClient().expectFileCount("/", 1, 10000));
        
        latch.await(getTimeout(), TimeUnit.MILLISECONDS);
        assertEquals(TEST_MESSAGE, message.get());                
        
        // give Mule some time to disconnect from the FTP server
        Thread.sleep(500);
    }

    protected static class FunctionalEventCallback implements EventCallback
    {
        private CountDownLatch latch;
        private AtomicReference message;

        public FunctionalEventCallback(CountDownLatch latch, AtomicReference message)
        {
            super();
            this.latch = latch;
            this.message = message;
        }
        
        public synchronized void eventReceived(MuleEventContext context, Object component)
        {
            try
            {
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
                throw new IllegalStateException(e);
            }
        }
    }
}
