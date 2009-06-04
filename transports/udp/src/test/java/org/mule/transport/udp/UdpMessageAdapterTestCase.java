/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.udp;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.transport.MessageAdapter;
import org.mule.transport.AbstractMessageAdapterTestCase;

import java.net.DatagramPacket;
import java.util.Arrays;

import org.apache.commons.lang.SerializationUtils;

public class UdpMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{

    public Object getValidMessage() throws Exception
    {
        return new DatagramPacket(TEST_MESSAGE.getBytes(), TEST_MESSAGE.length());
    }

    public MessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new UdpMessageAdapter(payload);
    }

    public void testMessageRetrieval() throws Exception
    {
        Object message = getValidMessage();
        MessageAdapter adapter = createAdapter(message);
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(adapter);
        assertEquals(new String(((DatagramPacket)message).getData()), muleMessage.getPayloadAsString());
        byte[] bytes = muleMessage.getPayloadAsBytes();
        assertNotNull(bytes);

        String stringMessage = muleMessage.getPayloadAsString();
        assertNotNull(stringMessage);

        assertNotNull(adapter.getPayload());

        try
        {
            adapter = createAdapter(getInvalidMessage());
            fail("Message adapter should throw exception if an invalid messgae is set");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    public void testSerialization() throws Exception
    {
        MessageAdapter messageAdapter = createAdapter(getValidMessage());
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(messageAdapter);

        byte[] serializedMessage = SerializationUtils.serialize(muleMessage);

        DefaultMuleMessage readMessage = 
            (DefaultMuleMessage) SerializationUtils.deserialize(serializedMessage);
        assertNotNull(readMessage.getAdapter());

        MessageAdapter readMessageAdapter = readMessage.getAdapter();
        assertTrue(readMessageAdapter instanceof UdpMessageAdapter);
        assertTrue(Arrays.equals(TEST_MESSAGE.getBytes(), 
            (byte[]) readMessageAdapter.getPayload()));
    }

}
