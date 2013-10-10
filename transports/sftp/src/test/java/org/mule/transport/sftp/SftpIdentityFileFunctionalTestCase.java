/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import org.mule.api.MuleEventContext;
import org.mule.module.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;

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

        MuleClient client = new MuleClient(muleContext);

        // Ensure that no other files exists
        // cleanupRemoteFtpDirectory(client, INBOUND_ENDPOINT_NAME);

        Map<?, ?> properties = new HashMap<Object, Object>();
        // properties.put("filename", "foo.bar");

        Object component = getComponent("testComponent");
        assertTrue("FunctionalTestComponent expected", component instanceof FunctionalTestComponent);
        FunctionalTestComponent ftc = (FunctionalTestComponent) component;
        assertNotNull(ftc);

        ftc.setEventCallback(callback);

        logger.debug("before dispatch");
        // Send an file to the SFTP server, which the inbound-endpoint then can pick
        // up
        client.dispatch(getAddressByEndpoint(client, INBOUND_ENDPOINT_NAME), TEST_MESSAGE, properties);
        logger.debug("before retrieve");

        latch.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);

        assertEquals(TEST_MESSAGE, message.get());
    }
}
