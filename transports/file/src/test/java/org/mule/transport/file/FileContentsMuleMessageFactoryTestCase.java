/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.util.IOUtils;

import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FileContentsMuleMessageFactoryTestCase extends AbstractFileMuleMessageFactoryTestCase
{
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        fillTempFile();
    }

    private void fillTempFile() throws Exception
    {
        Writer writer = new FileWriter(tempFile);
        writer.write(TEST_MESSAGE);
        writer.close();
    }

    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new FileContentsMuleMessageFactory();
    }

    @Override
    public void testValidPayload() throws Exception
    {
        MuleMessageFactory factory = createMuleMessageFactory();
        
        Object payload = getValidTransportMessage();
        MuleMessage message = factory.create(payload, encoding, muleContext);
        assertNotNull(message);
        assertPayload(message);
    }
    
    @Test
    public void testPayloadFromInputStream() throws Exception
    {
        MuleMessageFactory factory = createMuleMessageFactory();
        
        InputStream stream = null;
        try
        {
            stream = new ReceiverFileInputStream(tempFile, false, null);
            MuleMessage message = factory.create(stream, encoding, muleContext);
            assertNotNull(message);
            
            // delete the file before accessing the payload to make sure it was properly converted
            // to byte[] by the message factory
            assertTrue(tempFile.delete());
            assertPayload(message);
        }
        finally
        {
            IOUtils.closeQuietly(stream);
        }
    }

    private void assertPayload(MuleMessage message)
    {
        byte[] expected = TEST_MESSAGE.getBytes();
        byte[] result = (byte[]) message.getPayload();
        assertTrue(Arrays.equals(expected, result));
    }
}
