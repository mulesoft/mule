/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformer.simple.ObjectToByteArray;
import org.mule.transformer.types.SimpleDataType;
import org.mule.util.store.DeserializationPostInitialisable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import edu.emory.mathcs.backport.java.util.Arrays;

import org.apache.commons.lang.SerializationUtils;

public class DefaultMuleMessageSerializationTestCase extends AbstractMuleTestCase
{
    private static final String INNER_TEST_MESSAGE = "TestTestTestHello";
    
    public void testSerializablePayload() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        message.setProperty("foo", "bar");
        
        MuleMessage deserializedMessage = serializationRoundtrip(message);
        
        assertEquals(TEST_MESSAGE, deserializedMessage.getPayload());
        assertEquals("bar", deserializedMessage.getProperty("foo"));
    }
    
    public void testNonSerializablePayload() throws Exception
    {
        // add a transformer to the registry that can convert a NonSerializable to byte[]. This
        // will be used during Serialization
        muleContext.getRegistry().registerTransformer(new NonSerializableToByteArray());
        
        MuleMessage message = new DefaultMuleMessage(new NonSerializable(), muleContext);
        message.setProperty("foo", "bar");
        
        MuleMessage deserializedMessage = serializationRoundtrip(message);

        assertTrue(deserializedMessage.getPayload() instanceof byte[]);
        assertEquals(INNER_TEST_MESSAGE, deserializedMessage.getPayloadAsString());
    }
    
    public void testStreamPayloadSerialization() throws Exception
    {
        InputStream stream = new ByteArrayInputStream(TEST_MESSAGE.getBytes());
        MuleMessage message = new DefaultMuleMessage(stream, muleContext);
        message.setProperty("foo", "bar");
        
        MuleMessage deserializedMessage = serializationRoundtrip(message);
        
        assertEquals(byte[].class, deserializedMessage.getPayload().getClass());
        byte[] payload = (byte[]) deserializedMessage.getPayload();
        assertTrue(Arrays.equals(TEST_MESSAGE.getBytes(), payload));
    }

    private MuleMessage serializationRoundtrip(MuleMessage message) throws Exception
    {
        byte[] serialized = SerializationUtils.serialize(message);
        MuleMessage deserializedMessage = (MuleMessage) SerializationUtils.deserialize(serialized);
        DeserializationPostInitialisable.Implementation.init(deserializedMessage, muleContext);
        return deserializedMessage;
    }

    static class NonSerializable
    {
        private String content = INNER_TEST_MESSAGE;
    
        String getContent()
        {
            return content;
        }
    }
    
    static class NonSerializableToByteArray extends ObjectToByteArray
    {
        public NonSerializableToByteArray()
        {
            super();
            registerSourceType(new SimpleDataType<NonSerializable>(NonSerializable.class));
            setReturnClass(byte[].class);
        }
        
        @Override
        public Object doTransform(Object src, String encoding) throws TransformerException
        {
            String content = ((NonSerializable) src).getContent();
            return content.getBytes();
        }
    }
}
