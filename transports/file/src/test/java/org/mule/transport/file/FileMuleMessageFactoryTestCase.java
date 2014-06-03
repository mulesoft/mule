/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;

import java.io.File;

import org.junit.Test;

public class FileMuleMessageFactoryTestCase extends AbstractFileMuleMessageFactoryTestCase
{
    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new FileMuleMessageFactory();
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

        MuleMessage message = factory.create(getValidTransportMessage(), encoding, muleContext);
        assertNotNull(message);
        assertMessageProperties(message);
    }

    @Test
    public void testCreateMessageFromStream() throws Exception
    {
        MuleMessageFactory factory = createMuleMessageFactory();

        ReceiverFileInputStream stream = new ReceiverFileInputStream(tempFile, false, null);
        MuleMessage message = factory.create(stream, encoding, muleContext);
        assertNotNull(message);
        assertMessageProperties(message);
    }

    @Test
    public void testCloseSeveralTimes() throws Exception
    {
        MuleMessageFactory factory = createMuleMessageFactory();
        File moveTo = tempFolder.newFile("moveTo.tmp");
        moveTo.deleteOnExit();
        InputStreamCloseListener closeListener = mock(InputStreamCloseListener.class);
        ReceiverFileInputStream stream = new ReceiverFileInputStream(tempFile, false, moveTo, closeListener);
        MuleMessage message = factory.create(stream, encoding, muleContext);
        assertNotNull(message);
        stream.close();
        stream.close();
        verify(closeListener, times(1)).fileClose(tempFile);
        verify(closeListener, times(0)).fileClose(moveTo);
    }

    private void assertMessageProperties(MuleMessage message)
    {
        assertEquals(tempFile.getName(), message.getInboundProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME));
        assertEquals(tempFile.getParent(), message.getInboundProperty(FileConnector.PROPERTY_DIRECTORY));
        assertEquals(0l, message.getInboundProperty(FileConnector.PROPERTY_FILE_SIZE));
    }
}


