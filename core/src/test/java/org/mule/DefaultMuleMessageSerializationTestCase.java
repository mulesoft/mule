/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.simple.ObjectToByteArray;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.SimpleDataType;
import org.mule.util.StringDataSource;
import org.mule.util.store.DeserializationPostInitialisable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import javax.activation.DataHandler;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultMuleMessageSerializationTestCase extends AbstractMuleContextTestCase
{
    private static final String INNER_TEST_MESSAGE = "TestTestTestHello";

    @Test
    public void testSerializablePayload() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
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

        MuleMessage message = new DefaultMuleMessage(new NonSerializable(), muleContext);
        message.setOutboundProperty("foo", "bar");

        MuleMessage deserializedMessage = serializationRoundtrip(message);

        assertTrue(deserializedMessage.getPayload() instanceof byte[]);
        assertEquals(INNER_TEST_MESSAGE, deserializedMessage.getPayloadAsString());
    }

    @Test
    public void testStreamPayloadSerialization() throws Exception
    {
        InputStream stream = new ByteArrayInputStream(TEST_MESSAGE.getBytes());
        MuleMessage message = new DefaultMuleMessage(stream, muleContext);
        message.setOutboundProperty("foo", "bar");

        MuleMessage deserializedMessage = serializationRoundtrip(message);

        assertEquals(byte[].class, deserializedMessage.getPayload().getClass());
        byte[] payload = (byte[]) deserializedMessage.getPayload();
        assertTrue(Arrays.equals(TEST_MESSAGE.getBytes(), payload));
    }

    @Test
    @Ignore("see MULE-2964")
    public void testAttachments() throws Exception
    {
        String attachmentName = "the-attachment";

        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        DataHandler dataHandler = new DataHandler(new StringDataSource("attachment content"));
        message.addAttachment(attachmentName, dataHandler);

        MuleMessage deserializedMessage = serializationRoundtrip(message);

        assertEquals(1, deserializedMessage.getAttachmentNames().size());
        assertTrue(deserializedMessage.getAttachmentNames().contains(attachmentName));
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
            setReturnDataType(DataTypeFactory.BYTE_ARRAY);
        }

        @Override
        public Object doTransform(Object src, String outputEncoding) throws TransformerException
        {
            String content = ((NonSerializable) src).getContent();
            return content.getBytes();
        }
    }
}
