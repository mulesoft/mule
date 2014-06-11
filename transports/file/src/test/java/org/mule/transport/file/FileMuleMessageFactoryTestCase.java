/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;

import java.io.File;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FileMuleMessageFactoryTestCase extends AbstractFileMuleMessageFactoryTestCase
{
    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new FileMuleMessageFactory(muleContext);
    }

    @Override
    protected Object getValidTransportMessage()
    {
        return tempFile;
    }

    @Test
    public void testMessageProperties() throws Exception
    {
        MuleMessageFactory factory = createMuleMessageFactory();

        MuleMessage message = factory.create(getValidTransportMessage(), encoding);
        assertNotNull(message);
        assertMessageProperties(message);
    }

    @Test
    public void testCreateMessageFromStream() throws Exception
    {
        MuleMessageFactory factory = createMuleMessageFactory();

        ReceiverFileInputStream stream = new ReceiverFileInputStream(tempFile, false, null);
        MuleMessage message = factory.create(stream, encoding);
        assertNotNull(message);
        assertMessageProperties(message);
    }

    @Test
    public void testCloseSeveralTimes() throws Exception
    {
        MuleMessageFactory factory = createMuleMessageFactory();
        File moveTo = tempFolder.newFile("moveTo.tmp");
        moveTo.deleteOnExit();
        ReceiverFileInputStream mockStream = new ReceiverFileInputStream(tempFile, false, moveTo);
        MuleMessage message = factory.create(mockStream, encoding);
        assertNotNull(message);

        assertTrue(tempFile.exists());

        mockStream.close();
        assertTrue(!tempFile.exists());

        mockStream.close(); // Ensure second close works ok
    }

    private void assertMessageProperties(MuleMessage message)
    {
        assertEquals(tempFile.getName(), message.getOutboundProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME));
        assertEquals(tempFile.getParent(), message.getOutboundProperty(FileConnector.PROPERTY_DIRECTORY));
        assertEquals(0l, message.getOutboundProperty(FileConnector.PROPERTY_FILE_SIZE));
    }
}


