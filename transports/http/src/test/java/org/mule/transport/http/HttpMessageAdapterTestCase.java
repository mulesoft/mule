/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;


import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transport.MessageAdapter;
import org.mule.transport.AbstractMessageAdapterTestCase;
import org.mule.util.store.DeserializationPostInitialisable;

import java.io.ByteArrayInputStream;

import org.apache.commons.lang.SerializationUtils;

public class HttpMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{

    private byte[] message = TEST_MESSAGE.getBytes();

    public Object getValidMessage() throws Exception
    {
        return message;
    }

    public MessageAdapter createAdapter(Object payload) throws MuleException
    {
        return new HttpMessageAdapter(payload);
    }
 
    /**
     * Ensure that if the HttpMessageAdaptor is serialized the payload can still
     * be read from the original instance that was used for serialization. Also
     * ensure that when deserializing the payload is the same.
     */
    public void testStringPayloadSerialization() throws Exception 
    {
        String payload = "TEST";
        MuleMessage message = new DefaultMuleMessage(new HttpMessageAdapter(payload), muleContext);
        
        byte[] serialized = SerializationUtils.serialize(message);
        
        MuleMessage deserializedMessage = (MuleMessage) SerializationUtils.deserialize(serialized);
        DeserializationPostInitialisable.Implementation.init(deserializedMessage, muleContext);
        
        assertEquals(payload, deserializedMessage.getPayloadAsString());
        assertEquals(payload, message.getPayloadAsString());
    }

    /**
     * Ensure that if the HttpMessageAdaptor is serialized the stream payload
     * can still be read from the original instance that was used for
     * serialization. Also ensure that when deserializing the payload is the same.
     * This reproduces MULE-4459.
     */
    public void testStreamPayloadSerialization() throws Exception 
    {
        String payload = "TEST";
        ByteArrayInputStream stream = new ByteArrayInputStream(payload.getBytes());
        HttpMessageAdapter messageAdapter = new HttpMessageAdapter(stream);
        MuleMessage message = new DefaultMuleMessage(messageAdapter, muleContext);
        
        byte[] serialized = SerializationUtils.serialize(message);

        MuleMessage deserializedMessage = (MuleMessage) SerializationUtils.deserialize(serialized);
        DeserializationPostInitialisable.Implementation.init(deserializedMessage, muleContext);
        
        assertEquals(payload, deserializedMessage.getPayloadAsString());
        assertEquals(payload, message.getPayloadAsString());
    }

}
