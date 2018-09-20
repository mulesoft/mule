/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertEquals;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;

import org.junit.Test;

public class SftpWriteFileTestCase extends AbstractSftpFunctionalTestCase {
    
    @Override
    protected String getConfigFile()
    {
        return "mule-sftp-write-file-config.xml";
    }
    
    @Test
    public void appendFile() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.send("vm://append", "hello", null);
        client.send("vm://append", " world", null);
        MuleMessage message = client.request("file://testdir/append.txt", RECEIVE_TIMEOUT);
        assertEquals("hello world", message.getPayloadAsString());
    }

    @Test
    public void addSeqNo() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.send("vm://add-seq-no", "hello", null);
        client.send("vm://add-seq-no", " world", null);
        MuleMessage message = client.request("file://testdir/append.txt", RECEIVE_TIMEOUT);
        assertEquals("hello", message.getPayloadAsString());
        message = client.request("file://testdir/append_1.txt", RECEIVE_TIMEOUT);
        assertEquals(" world", message.getPayloadAsString());
    }

    @Test
    public void addSeqNoUsingTempDirOutbound() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.send("vm://add-seq-no-with-temp-dir-outbound", "hello", null);
        client.send("vm://add-seq-no-with-temp-dir-outbound", " world", null);
        MuleMessage message = client.request("file://testdir/append.txt", RECEIVE_TIMEOUT);
        assertEquals("hello", message.getPayloadAsString());
        message = client.request("file://testdir/append_1.txt", RECEIVE_TIMEOUT);
        assertEquals(" world", message.getPayloadAsString());
    }

    @Test
    public void appendFileUsingTempDirOutbound() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.send("vm://append-with-temp-dir-outbound", "hello", null);
        client.send("vm://append-with-temp-dir-outbound", " world", null);
        MuleMessage message = client.request("file://testdir/append.txt", RECEIVE_TIMEOUT);
        assertEquals("hello world", message.getPayloadAsString());
    }

    @Test
    public void overwriteFile() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.send("vm://overwrite", "hello", null);
        client.send("vm://overwrite", "world", null);
        MuleMessage message = client.request("file://testdir/overwrite.txt", RECEIVE_TIMEOUT);
        assertEquals("world", message.getPayloadAsString());
    }

    @Test
    public void overwriteFileUsingTempDirOutbound() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.send("vm://overwrite-with-temp-dir-outbound", "hello", null);
        client.send("vm://overwrite-with-temp-dir-outbound", "world", null);
        MuleMessage message = client.request("file://testdir/overwrite.txt", RECEIVE_TIMEOUT);
        assertEquals("hello", message.getPayloadAsString());
    }

}
