/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleEventContext;
import org.mule.api.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class FtpFunctionalTestCase extends AbstractFtpServerTestCase
{
    public FtpFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "ftp-functional-test-service.xml"},
            {ConfigVariant.FLOW, "ftp-functional-test-flow.xml"}
        });
    }

    @Test
    public void testSendAndRequest() throws Exception
    {
        sendAndRequest(TEST_MESSAGE);
    }

    @Test
    public void testSendAndRequestEmptyFile() throws Exception
    {
        sendAndRequest("");
    }

    private void sendAndRequest(String inputMessage) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> message = new AtomicReference<String>();

        Object component = getComponent("testComponent");
        assertNotNull(component);
        assertTrue("FunctionalTestComponent expected", component instanceof FunctionalTestComponent);
        FunctionalTestComponent ftc = (FunctionalTestComponent) component;
        ftc.setEventCallback(new FunctionalEventCallback(latch, message));

        MuleClient client = muleContext.getClient();
        client.dispatch(getMuleFtpEndpoint(), inputMessage, null);

        // TODO DZ: need a reliable way to check the file once it's been written to
        // the ftp server. Currently, once mule processes the ftp'd file, it
        // auto-deletes it, so we can't check it
        //assertTrue(getFtpClient().expectFileCount("/", 1, 10000));

        latch.await(getTimeout(), TimeUnit.MILLISECONDS);
        assertEquals(inputMessage, message.get());
    }

    protected static class FunctionalEventCallback implements EventCallback
    {
        private CountDownLatch latch;
        private AtomicReference<String> message;

        public FunctionalEventCallback(CountDownLatch latch, AtomicReference<String> message)
        {
            super();
            this.latch = latch;
            this.message = message;
        }

        @Override
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
