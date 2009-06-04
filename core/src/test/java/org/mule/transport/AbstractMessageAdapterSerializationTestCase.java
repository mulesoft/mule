/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.DefaultMuleMessage;
import org.mule.api.transport.MessageAdapter;
import org.mule.tck.AbstractMuleTestCase;

import org.apache.commons.lang.SerializationUtils;

public abstract class AbstractMessageAdapterSerializationTestCase extends AbstractMuleTestCase
{
    protected static final String PAYLOAD = "Hello Mule";    
    protected static final String STRING_PROPERTY_KEY = "string";
    protected static final String STRING_PROPERTY_VALUE = "hello";
    
    public void testMessageAdapterSerialization() throws Exception
    {
        MessageAdapter messageAdapter = createAndCheckMessageAdapter();
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(messageAdapter);

        byte[] serializedMessage = SerializationUtils.serialize(muleMessage);
        
        DefaultMuleMessage readMessage = 
            (DefaultMuleMessage) SerializationUtils.deserialize(serializedMessage);
        assertNotNull(readMessage.getAdapter());
        assertTrue(readMessage.getAdapter() instanceof DefaultMessageAdapter);

        DefaultMessageAdapter readAdapter = (DefaultMessageAdapter) readMessage.getAdapter();
        byte[] payload = (byte[]) readAdapter.getPayload();
        assertEquals(PAYLOAD, new String(payload));
        assertEquals(STRING_PROPERTY_VALUE, readAdapter.getProperty(STRING_PROPERTY_KEY));
        
        doAdditionalAssertions(readAdapter);
    }

    private MessageAdapter createAndCheckMessageAdapter() throws Exception
    {
        MessageAdapter messageAdapter = createMessageAdapter();
        
        assertEquals(STRING_PROPERTY_VALUE, messageAdapter.getProperty(STRING_PROPERTY_KEY));
        
        return messageAdapter;
    }
    
    protected abstract MessageAdapter createMessageAdapter() throws Exception;

    /**
     * Subclasses can override this method to perform additional checks on the message adapter
     */
    protected void doAdditionalAssertions(DefaultMessageAdapter messageAdapter)
    {
        // empty, subclasses can override
    }
    
}


