/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;

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
        
    public void testMessageProperties() throws Exception
    {
        MuleMessageFactory factory = createMuleMessageFactory();
        
        MuleMessage message = factory.create(getValidTransportMessage(), encoding);
        assertNotNull(message);
        assertMessageProperties(message);
    }
    
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
            message.getProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME));
        assertEquals(tempFile.getParent(), message.getProperty(FileConnector.PROPERTY_DIRECTORY));
    }
}


