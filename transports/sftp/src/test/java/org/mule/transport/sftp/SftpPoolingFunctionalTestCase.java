/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.MuleEventContext;
import org.mule.api.client.MuleClient;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.functional.EventCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * <code>SftpPoolingFunctionalTestCase</code> tests sending an receiving multiple
 * small text files.
 */
public class SftpPoolingFunctionalTestCase extends AbstractSftpTestCase
{
    private static final long TIMEOUT = 30000;

    private List<String> sendFiles;
    private List<String> receiveFiles;

    private int nrOfFiles = 100;

    public SftpPoolingFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "mule-pooling-test-config-service.xml"},
            {ConfigVariant.FLOW, "mule-pooling-test-config-flow.xml"}
        });
    }

    @Test
    public void testSftpConfig() throws Exception
    {
        SftpConnector c = (SftpConnector) muleContext.getRegistry().lookupConnector("sftp-pool");
        assertEquals(3, c.getMaxConnectionPoolSize());
        assertEquals(true, c.useConnectionPool());

        SftpConnector c2 = (SftpConnector) muleContext.getRegistry().lookupConnector("sftp-no-pool");
        assertEquals(false, c2.useConnectionPool());
    }

    @Test
    public void testSendAndReceiveMultipleFiles() throws Exception
    {
        sendFiles = new ArrayList<String>();

        for (int i = 1; i <= nrOfFiles; i++)
        {
            sendFiles.add("file" + i);
        }
        sendAndReceiveFiles();
    }

    protected void sendAndReceiveFiles() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(sendFiles.size());
        final AtomicInteger loopCount = new AtomicInteger(0);

        receiveFiles = new ArrayList<String>();

        EventCallback callback = new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {

                String filename = context.getMessage().getProperty(SftpConnector.PROPERTY_ORIGINAL_FILENAME, PropertyScope.INBOUND);
                SftpInputStream inputStream = null;
                try
                {
                    logger.info("called " + loopCount.incrementAndGet() + " times. Filename = " + filename);

                    // This is not thread safe! (it should be safe if
                    // synchronous="true" is used)
                    // FunctionalTestComponent ftc = (FunctionalTestComponent)
                    // component;
                    // inputStream = (SftpInputStream) ftc.getLastReceivedMessage();

                    // Use this instead!
                    inputStream = (SftpInputStream) context.getMessage().getPayload();
                    String o = IOUtils.toString(inputStream);
                    if (sendFiles.contains(o))
                    {
                        logger.info("The received file was added. Received: '" + o + "'");
                        receiveFiles.add(o);
                    }
                    else
                    {
                        fail("The received file was not sent. Received: '" + o + "'");
                    }

                    latch.countDown();
                }
                catch (IOException e)
                {
                    logger.error("Error occured while processing callback for file=" + filename, e);
                    throw e;
                }
                finally
                {
                    if (inputStream != null)
                    {
                        inputStream.close();
                    }
                }
            }
        };

        getFunctionalTestComponent("receiving").setEventCallback(callback);

        MuleClient client = muleContext.getClient();
        for (String sendFile : sendFiles)
        {
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(SftpConnector.PROPERTY_FILENAME, sendFile + ".txt");

            client.dispatch("vm://test.upload", sendFile, props);
        }

        latch.await(TIMEOUT, TimeUnit.MILLISECONDS);

        logger.debug("Number of files sent: " + sendFiles.size());
        logger.debug("Number of files received: " + receiveFiles.size());

        // This makes sure we received the same number of files we sent, and that
        // the content was a match (since only matched content gets on the
        // receiveFiles ArrayList)
        assertTrue("expected : " + sendFiles.size() + " but got " + receiveFiles.size(),
            sendFiles.size() == receiveFiles.size());
    }
}
