/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MutableMuleMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.simple.ObjectToByteArray;
import org.mule.runtime.core.util.store.DeserializationPostInitialisable;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

public class DefaultMuleMessageSerializationTestCase extends AbstractMuleContextTestCase
{
    private static final String INNER_TEST_MESSAGE = "TestTestTestHello";

    @Test
    public void testSerializablePayload() throws Exception
    {
        // TODO MULE-9856 Replace with the builder
        MutableMuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        message.setOutboundProperty("foo", "bar");

        MuleMessage deserializedMessage = serializationRoundtrip(message);

        assertEquals(TEST_MESSAGE, deserializedMessage.getPayload());
        assertEquals("bar", deserializedMessage.getOutboundProperty("foo"));
    }

    @Test
    public void testNonSerializablePayload() throws Exception
    {
        // add a transformer to the registry that can convert a NonSerializable to byte[]. This
        // will be used during Serialization
        muleContext.getRegistry().registerTransformer(new NonSerializableToByteArray());

        // TODO MULE-9856 Replace with the builder
        MutableMuleMessage message = new DefaultMuleMessage(new NonSerializable(), muleContext);
        message.setOutboundProperty("foo", "bar");

        RequestContext.setEvent(new DefaultMuleEvent(message, getTestFlow()));
        MuleMessage deserializedMessage = serializationRoundtrip(message);

        assertTrue(deserializedMessage.getPayload() instanceof byte[]);
        assertEquals(INNER_TEST_MESSAGE, getPayloadAsString(deserializedMessage));
    }

    @Test
    public void testStreamPayloadSerialization() throws Exception
    {
        InputStream stream = new ByteArrayInputStream(TEST_MESSAGE.getBytes());
        // TODO MULE-9856 Replace with the builder
        MutableMuleMessage message = new DefaultMuleMessage(stream, muleContext);
        message.setOutboundProperty("foo", "bar");

        RequestContext.setEvent(new DefaultMuleEvent(message, getTestFlow()));
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
            registerSourceType(DataType.fromType(NonSerializable.class));
            setReturnDataType(DataType.BYTE_ARRAY);
        }

        @Override
        public Object doTransform(Object src, Charset outputEncoding) throws TransformerException
        {
            String content = ((NonSerializable) src).getContent();
            return content.getBytes();
        }
    }
}
