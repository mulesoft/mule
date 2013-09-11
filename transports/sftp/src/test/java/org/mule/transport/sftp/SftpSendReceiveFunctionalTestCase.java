/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.MuleEventContext;
import org.mule.api.client.MuleClient;
import org.mule.tck.functional.EventCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * <code>SendReceiveFunctionalTestCase</code> tests sending an receiving multiple
 * small text files.
 */
public class SftpSendReceiveFunctionalTestCase extends AbstractSftpTestCase
{
    private static final long TIMEOUT = 30000;

    private ArrayList<String> sendFiles;
    private ArrayList<String> receiveFiles;

    private int nrOfFiles = 8;

    public SftpSendReceiveFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "mule-send-receive-test-config-service.xml"},
            {ConfigVariant.FLOW, "mule-send-receive-test-config-flow.xml"}});
    }

    @Test
    public void testSendAndReceiveSingleFile() throws Exception
    {
        sendFiles = new ArrayList<String>();

        sendFiles.add("file created on " + new Date().getTime());

        sendAndReceiveFiles();
    }

    // Test Mule-1477 (an old VFS Connector issue, but test anyway).
    @Test
    public void testSendAndReceiveEmptyFile() throws Exception
    {
        sendFiles = new ArrayList<String>();

        sendFiles.add("");

        sendAndReceiveFiles();
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

        assertTrue("muleContext is not started", muleContext.isStarted());
        receiveFiles = new ArrayList<String>();

        EventCallback callback = new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                logger.info("called " + loopCount.incrementAndGet() + " times");

                SftpInputStream inputStream = (SftpInputStream) context.getMessage().getPayload();
                String o = IOUtils.toString(inputStream);
                if (sendFiles.contains(o))
                {
                    receiveFiles.add(o);
                }
                else
                {
                    fail("The received file was not sent. Received: '" + o + "'");
                }

                latch.countDown();
                inputStream.close();
            }
        };

        getFunctionalTestComponent("receiving").setEventCallback(callback);

        MuleClient client = muleContext.getClient();
        for (String sendFile : sendFiles)
        {
            HashMap<String, Object> props = new HashMap<String, Object>();
            props.put(SftpConnector.PROPERTY_FILENAME, sendFile + ".txt");
            client.dispatch("vm://test.upload", sendFile, props);
        }

        latch.await(TIMEOUT, TimeUnit.MILLISECONDS);

        logger.debug("Number of files sent: " + sendFiles.size());
        logger.debug("Number of files received: " + receiveFiles.size());

        // This makes sure we received the same number of files we sent, and that
        // the content was a match (since only matched content gets on the
        // receiveFiles ArrayList)
        assertTrue("expected " + sendFiles.size() + " but got " + receiveFiles.size(),
            sendFiles.size() == receiveFiles.size());
    }
}
