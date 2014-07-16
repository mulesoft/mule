/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleEventContext;
import org.mule.api.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * <code>LargeFileReceiveFunctionalTestCase</code> tests receiving a large file
 * message from an sftp service.
 *
 * @author Lennart Häggkvist
 */
public class SftpIdentityFileFunctionalTestCase extends AbstractSftpTestCase
{
    private static final int DEFAULT_TIMEOUT = 10000;

    // Increase this to be a little larger than expected download time
    private static final String INBOUND_ENDPOINT_NAME = "inboundEndpoint";

    public SftpIdentityFileFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "mule-sftp-identity-file-config-service.xml"},
            {ConfigVariant.FLOW, "mule-sftp-identity-file-config-flow.xml"}
        });
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        initEndpointDirectory(INBOUND_ENDPOINT_NAME);
    }

    // Downloads large file in the remote directory specified in config
    @Test
    public void testIdentityFile() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> message = new AtomicReference<String>();
        final AtomicInteger loopCount = new AtomicInteger(0);

        EventCallback callback = new EventCallback()
        {
            @Override
            public synchronized void eventReceived(MuleEventContext context, Object component)
            {
                try
                {
                    logger.info("called " + loopCount.incrementAndGet() + " times");
                    // without this we may have problems with the many repeats
                    if (1 == latch.getCount())
                    {
                        String o = IOUtils.toString((SftpInputStream) context.getMessage().getPayload());
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

        MuleClient client = muleContext.getClient();

        // Ensure that no other files exists
        // cleanupRemoteFtpDirectory(client, INBOUND_ENDPOINT_NAME);

        Map<String, Object> properties = new HashMap<String, Object>();
        // properties.put("filename", "foo.bar");

        Object component = getComponent("testComponent");
        assertTrue("FunctionalTestComponent expected", component instanceof FunctionalTestComponent);
        FunctionalTestComponent ftc = (FunctionalTestComponent) component;
        assertNotNull(ftc);

        ftc.setEventCallback(callback);

        // Send an file to the SFTP server, which the inbound-endpoint then can pick up
        client.dispatch(getAddressByEndpoint(INBOUND_ENDPOINT_NAME), TEST_MESSAGE, properties);

        latch.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(TEST_MESSAGE, message.get());
    }
}
