/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.transport.ReceiveException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class FtpMessageRequesterTestCase extends AbstractFtpServerTestCase
{
    private static final String TEST_FILE_NAME = "test.txt";
    private static final String TEST_FILE_NAME_2 = "_test.txt";
    private static final String separator = "/";
    public FtpMessageRequesterTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.FLOW, "ftp-message-requester-test.xml"}
        });
    }

    @Test
    public void testMessageRequester() throws Exception
    {
        createFileOnFtpServer(TEST_FILE_NAME);

        MuleClient client = muleContext.getClient();
        MuleMessage message = client.request(getMuleFtpEndpoint(), getTimeout());
        assertNotNull(message);
        assertEquals(TEST_MESSAGE, message.getPayloadAsString());

        // verify that the file was deleted
        MuleMessage message2 = client.request(getMuleFtpEndpoint(), getTimeout());
        assertNull(message2);
    }

    @Test
    public void testMessageRequesterForSingleFile() throws Exception
    {
        createFileOnFtpServer(TEST_FILE_NAME);
        
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.request(getMuleFtpEndpoint() + separator + TEST_FILE_NAME, getTimeout());
        assertNotNull(message);
        assertEquals(TEST_MESSAGE, message.getPayloadAsString());
        assertEquals(TEST_FILE_NAME, message.getInboundProperty("originalFilename"));

        // verify that the file was deleted
        MuleMessage message2 = client.request(getMuleFtpEndpoint(), getTimeout());
        assertNull(message2);
    }

    @Test
    public void testMessageRequesterForSingleFileAmongMultipleFiles() throws Exception
    {
        createFileOnFtpServer(TEST_FILE_NAME);
        createFileOnFtpServer(TEST_FILE_NAME_2);
        
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.request(getMuleFtpEndpoint() + separator + TEST_FILE_NAME, getTimeout());
        assertNotNull(message);
        assertEquals(TEST_MESSAGE, message.getPayloadAsString());
        assertEquals(TEST_FILE_NAME, message.getInboundProperty("originalFilename"));
        
        // retrieve file 2
        MuleMessage message2 = client.request(getMuleFtpEndpoint() + separator + TEST_FILE_NAME_2, getTimeout());
        assertNotNull(message2);
        assertEquals(TEST_MESSAGE, message2.getPayloadAsString());
        assertEquals(TEST_FILE_NAME_2, message2.getInboundProperty("originalFilename"));
        
        // verify that all the files were deleted
        MuleMessage message3 = client.request(getMuleFtpEndpoint(), getTimeout());
        assertNull(message3);
        
    }
    @Test
    public void testMessageRequesterForSingleFileThatDoesNotExist() throws Exception
    {
        createFileOnFtpServer(TEST_FILE_NAME);
        
        MuleClient client = muleContext.getClient();
        try{
            
            MuleMessage message = client.request(getMuleFtpEndpoint() + separator + TEST_FILE_NAME_2, getTimeout());
        } catch(Exception e){
            Assert.assertTrue(e instanceof ReceiveException);
        }
        
    }
}
