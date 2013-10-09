/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file;

import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;

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

    private void assertMessageProperties(MuleMessage message)
    {
        assertEquals(tempFile.getName(),
            message.getOutboundProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME));
        assertEquals(tempFile.getParent(), message.getOutboundProperty(FileConnector.PROPERTY_DIRECTORY));
        assertEquals(0l, message.getOutboundProperty(FileConnector.PROPERTY_FILE_SIZE));
    }
}


