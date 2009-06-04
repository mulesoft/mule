/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.rmi;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.transport.MessageAdapter;
import org.mule.transport.AbstractMessageAdapterTestCase;

import org.apache.commons.lang.SerializationUtils;

public class RmiMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{

    public Object getValidMessage() throws Exception
    {
        return "Hello".getBytes();
    }

    public MessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new RmiMessageAdapter(payload);
    }

    @Override
    public Object getInvalidMessage()
    {
        return null;
    }

    public void testSerialization() throws Exception
    {
        MessageAdapter messageAdapter = createAdapter(TEST_MESSAGE);
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(messageAdapter);
        
        byte[] serializedMessage = SerializationUtils.serialize(muleMessage);

        DefaultMuleMessage readMessage = 
            (DefaultMuleMessage) SerializationUtils.deserialize(serializedMessage);
        assertNotNull(readMessage.getAdapter());

        MessageAdapter readMessageAdapter = readMessage.getAdapter();
        assertTrue(readMessageAdapter instanceof RmiMessageAdapter);
        assertEquals(TEST_MESSAGE, readMessageAdapter.getPayload());
    }

}
