/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.processor.MessageProcessor;
import org.mule.util.concurrent.Latch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class SftpAutoDeleteTestCase extends AbstractSftpFunctionalTestCase
{
    private static final String FILENAME = "file.txt";
    private static final String FILE_CONTENT = "File content";
    private static final String AUTO_DELETE_OFF = "autoDeleteOff";
    private static final String AUTO_DELETE_ON = "autoDeleteOn";

    private static Latch latch;
    private static MuleMessage  message;

    @Parameterized.Parameter(0)
    public String config;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"mule-sftp-auto-delete-implicit-connector.xml"},
                {"mule-sftp-auto-delete-explicit-connector.xml"}});
    }

    @Override
    protected String getConfigFile()
    {
        return config;
    }

    @Override
    protected void setUpTestData() throws IOException
    {
        latch = new Latch();
        sftpClient.changeWorkingDirectory("..");
        sftpClient.mkdir(AUTO_DELETE_OFF);
        sftpClient.mkdir(AUTO_DELETE_ON);
    }

    @After
    public void tearDownData() throws IOException
    {
        sftpClient.changeWorkingDirectory("..");
        sftpClient.recursivelyDeleteDirectory(AUTO_DELETE_OFF);
        sftpClient.recursivelyDeleteDirectory(AUTO_DELETE_ON);
    }

    @Test
    public void endpointAutoDeleteFalse() throws Exception
    {
        testDirectory(AUTO_DELETE_OFF);
        assertTrue(Arrays.asList(sftpClient.listFiles()).contains(FILENAME));
    }

    @Test
    public void endpointAutoDeleteTrue() throws Exception
    {
        testDirectory(AUTO_DELETE_ON);
        assertFalse(Arrays.asList(sftpClient.listFiles()).contains(FILENAME));
    }

    private void testDirectory(String directory) throws Exception
    {
        sftpClient.changeWorkingDirectory(directory);
        sftpClient.storeFile(FILENAME, new ByteArrayInputStream(FILE_CONTENT.getBytes()));
        MuleClient muleClient = muleContext.getClient();
        muleClient.dispatch(getUrl(directory), new DefaultMuleMessage(TEST_MESSAGE, muleContext));
        latch.await(1500, TimeUnit.MILLISECONDS);
        assertThat(message, notNullValue());
        assertThat(message.getPayloadAsString(), is(FILE_CONTENT));
    }

    private String getUrl(String directory)
    {
        return String.format("sftp://localhost:%s/%s", sftpPort.getNumber(), directory);
    }

    public static class LatchMessageProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            message = event.getMessage();
            latch.release();
            return event;
        }
    }
}
