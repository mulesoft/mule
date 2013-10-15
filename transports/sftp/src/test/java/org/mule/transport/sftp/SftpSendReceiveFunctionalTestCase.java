/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import org.mule.api.MuleEventContext;
import org.mule.module.client.MuleClient;
import org.mule.tck.functional.EventCallback;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

    @Override
    protected String getConfigResources()
    {
        return "mule-send-receive-test-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        initEndpointDirectory("inboundEndpoint");
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

        MuleClient client = new MuleClient(muleContext);
        assertTrue("muleContext is not started", muleContext.isStarted());
        receiveFiles = new ArrayList<String>();

        EventCallback callback = new EventCallback()
        {
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

        for (String sendFile : sendFiles)
        {
            HashMap<String, String> props = new HashMap<String, String>(1);
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
