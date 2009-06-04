/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.vm;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transport.MessageAdapter;
import org.mule.api.transport.MessageTypeNotSupportedException;
import org.mule.transport.AbstractMessageAdapterTestCase;
import org.mule.transport.DefaultMessageAdapter;

import org.apache.commons.lang.SerializationUtils;

/**
 * <code>VMMessageAdapterTestCase</code> TODO (document class)
 */
public class VMMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{

    public MessageAdapter createAdapter(Object payload) throws MessageTypeNotSupportedException
    {
        if (payload instanceof MuleMessage)
        {
            return new DefaultMessageAdapter(payload);
        }
        else
        {
            throw new MessageTypeNotSupportedException(payload, DefaultMessageAdapter.class);
        }
    }

    public Object getValidMessage() throws MuleException
    {
        return new DefaultMuleMessage(TEST_MESSAGE);
    }

    public Object getInvalidMessage()
    {
        return "Invalid message";
    }

    public void testSerialization() throws Exception
    {
        DefaultMuleMessage muleMessage = (DefaultMuleMessage) getValidMessage();

        byte[] serializedMessage = SerializationUtils.serialize(muleMessage);

        DefaultMuleMessage readMessage = 
            (DefaultMuleMessage) SerializationUtils.deserialize(serializedMessage);
        assertNotNull(readMessage.getAdapter());

        MessageAdapter readMessageAdapter = readMessage.getAdapter();
        assertTrue(readMessageAdapter instanceof DefaultMessageAdapter);
        assertEquals(TEST_MESSAGE, readMessageAdapter.getPayload());
    }

}
