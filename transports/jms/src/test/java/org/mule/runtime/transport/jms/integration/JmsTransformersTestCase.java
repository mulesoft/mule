/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.core.util.compression.CompressionStrategy;
import org.mule.runtime.core.util.compression.GZipCompression;
import org.mule.runtime.transport.jms.transformers.AbstractJmsTransformer;
import org.mule.runtime.transport.jms.transformers.JMSMessageToObject;
import org.mule.tck.testmodels.fruit.Orange;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.MapMessage;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;

/**
 * <code>JmsTransformersTestCase</code> Tests the JMS transformer implementations.
 */

public class JmsTransformersTestCase extends AbstractJmsFunctionalTestCase
{
    private Session session = null;

    @Override
    protected String getConfigFile()
    {
        return "integration/jms-transformers.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        session = getConnection(false, false).createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    @Override
    protected void doTearDown() throws Exception
    {
        RequestContext.setEvent(null);
        if (session != null)
        {
            session.close();
            session = null;
        }
    }

    @Test
    public void testTransformObjectMessage() throws Exception
    {
        RequestContext.setEvent(getTestEvent("test"));

        ObjectMessage oMsg = session.createObjectMessage();
        File f = FileUtils.newFile("/some/random/path");
        oMsg.setObject(f);
        AbstractJmsTransformer trans = createObject(JMSMessageToObject.class);
        Object result = trans.transform(oMsg);
        assertTrue("Transformed object should be a File", result.getClass().equals(File.class));

        AbstractJmsTransformer trans2 = new SessionEnabledObjectToJMSMessage(session);
        trans2.setReturnDataType(DataTypeFactory.create(ObjectMessage.class));
        initialiseObject(trans2);
        Object result2 = trans2.transform(f);
        assertTrue("Transformed object should be an object message", result2 instanceof ObjectMessage);
    }

    @Test
    public void testTransformTextMessage() throws Exception
    {
        RequestContext.setEvent(getTestEvent("test"));

        String text = "This is a test TextMessage";
        TextMessage tMsg = session.createTextMessage();
        tMsg.setText(text);

        AbstractJmsTransformer trans = createObject(JMSMessageToObject.class);
        Object result = trans.transform(tMsg);
        assertTrue("Transformed object should be a string", text.equals(result.toString()));

        AbstractJmsTransformer trans2 = new SessionEnabledObjectToJMSMessage(session);
        trans2.setReturnDataType(DataTypeFactory.create(TextMessage.class));
        initialiseObject(trans2);
        Object result2 = trans2.transform(text);
        assertTrue("Transformed object should be a TextMessage", result2 instanceof TextMessage);
    }

    @Test
    public void testTransformMapMessage() throws Exception
    {
        RequestContext.setEvent(getTestEvent("test"));

        Map p = new HashMap();
        p.put("Key1", "Value1");
        p.put("Key2", new byte[]{1,2,3});
        p.put("Key3", new Double(99.999));

        AbstractJmsTransformer trans = new SessionEnabledObjectToJMSMessage(session);
        trans.setReturnDataType(DataTypeFactory.create(MapMessage.class));
        initialiseObject(trans);
        Object result2 = trans.transform(p);
        assertTrue("Transformed object should be a MapMessage", result2 instanceof MapMessage);

        MapMessage mMsg = (MapMessage) result2;
        AbstractJmsTransformer trans2 = createObject(JMSMessageToObject.class);
        trans2.setReturnDataType(DataTypeFactory.create(Map.class));
        Object result = trans2.transform(mMsg);
        assertTrue("Transformed object should be a Map", result instanceof Map);

        Map m = (Map)result;
        assertEquals("Value1", m.get("Key1"));
        assertTrue(Arrays.equals(new byte[]{1,2,3}, (byte[])m.get("Key2")));
        assertEquals(new Double(99.999), m.get("Key3"));
    }

    @Test
    public void testTransformMapToObjectMessage() throws Exception
    {
        RequestContext.setEvent(getTestEvent("test"));

        Map p = new HashMap();
        p.put("Key1", "Value1");
        p.put("Key2", new byte[]{1,2,3});
        p.put("Key3", new Double(99.999));
        p.put("Key4", new Orange());

        AbstractJmsTransformer trans = new SessionEnabledObjectToJMSMessage(session);
        trans.setReturnDataType(DataTypeFactory.create(ObjectMessage.class));
        initialiseObject(trans);
        Object result2 = trans.transform(p);
        assertTrue("Transformed object should be a ObjectMessage", result2 instanceof ObjectMessage);

        ObjectMessage oMsg = (ObjectMessage) result2;
        AbstractJmsTransformer trans2 = createObject(JMSMessageToObject.class);
        trans2.setReturnDataType(DataTypeFactory.create(Map.class));
        Object result = trans2.transform(oMsg);
        assertTrue("Transformed object should be a Map", result instanceof Map);

        Map m = (Map)result;
        assertEquals("Value1", m.get("Key1"));
        assertTrue(Arrays.equals(new byte[]{1,2,3}, (byte[])m.get("Key2")));
        assertEquals(new Double(99.999), m.get("Key3"));
        assertEquals(new Orange(), m.get("Key4"));
    }

    @Test
    public void testTransformByteMessage() throws Exception
    {
        RequestContext.setEvent(getTestEvent("test"));

        AbstractJmsTransformer trans = new SessionEnabledObjectToJMSMessage(session);
        trans.setReturnDataType(DataTypeFactory.create(BytesMessage.class));
        initialiseObject(trans);
        String text = "This is a test BytesMessage";
        Object result2 = trans.transform(text.getBytes());
        assertTrue("Transformed object should be a BytesMessage", result2 instanceof BytesMessage);

        AbstractJmsTransformer trans2 = createObject(JMSMessageToObject.class);
        trans2.setReturnDataType(DataTypeFactory.BYTE_ARRAY);
        BytesMessage bMsg = (BytesMessage) result2;
        Object result = trans2.transform(bMsg);
        assertTrue("Transformed object should be a byte[]", result instanceof byte[]);
        String res = new String((byte[]) result);
        assertEquals("Source and result should be equal", text, res);
    }

    @Test
    public void testTransformStreamMessage() throws Exception
    {
        RequestContext.setEvent(getTestEvent("test"));

        String text = "Test Text";
        int i = 97823;
        double d = 0923.2143E124;
        List<Object> list = new ArrayList<Object>();
        list.add(new Integer(i));
        list.add(new Double(d));
        list.add(text);

        StreamMessage message = session.createStreamMessage();
        message.writeString(text);
        message.writeInt(i);
        message.writeDouble(d);
        message.reset();

        AbstractJmsTransformer trans = createObject(JMSMessageToObject.class);
        Object transformedObject = trans.transform(message);
        assertTrue("Transformed object should be a List", transformedObject instanceof List);

        final List<?> result = (List<?>) transformedObject;
        String newText = (String) result.get(0);
        Integer newI = (Integer) result.get(1);
        Double newD = (Double) result.get(2);
        assertEquals(i, newI.intValue());
        assertEquals(new Double(d), newD);
        assertEquals(text, newText);
    }

    // The following test was disabled for ActiveMQ 3.x because ActiveMQ 3.2.4
    // unconditionally uncompresses BytesMessages for reading, even if it is not
    // supposed to do so (the layer doing the message reading seems to have no access
    // to the Broker configuration and seems to assume that compressed data was
    // compressed by ActiveMQ for more efficient wire transport).
    // This was fixed in 4.x.
    // For more information why this was VERY BAD read:
    // http://en.wikipedia.org/wiki/Zip_of_death
    @Test
    public void testCompressedBytesMessage() throws Exception
    {
        RequestContext.setEvent(getTestEvent("test"));

        // use GZIP
        CompressionStrategy compressor = new GZipCompression();

        // create compressible data
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < 5000; i++)
        {
            baos.write(i);
        }

        byte[] originalBytes = baos.toByteArray();
        byte[] compressedBytes = compressor.compressByteArray(originalBytes);
        assertTrue("Source compressedBytes should be compressed", compressor.isCompressed(compressedBytes));

        // now create a BytesMessage from the compressed byte[]
        AbstractJmsTransformer trans = new SessionEnabledObjectToJMSMessage(session);
        trans.setReturnDataType(DataTypeFactory.create(BytesMessage.class));
        initialiseObject(trans);
        Object result2 = trans.transform(compressedBytes);
        assertTrue("Transformed object should be a Bytes message", result2 instanceof BytesMessage);

        // check whether the BytesMessage contains the compressed bytes
        BytesMessage intermediate = (BytesMessage) result2;
        intermediate.reset();
        byte[] intermediateBytes = new byte[(int) (intermediate.getBodyLength())];
        int intermediateSize = intermediate.readBytes(intermediateBytes);
        assertTrue("Intermediate bytes must be compressed", compressor.isCompressed(intermediateBytes));
        assertTrue("Intermediate bytes must be equal to compressed source", Arrays.equals(compressedBytes,
            intermediateBytes));
        assertEquals("Intermediate bytes and compressed source must have same size", compressedBytes.length,
            intermediateSize);

        // now test the other way around: getting the byte[] from a manually created
        // BytesMessage
        AbstractJmsTransformer trans2 = createObject(JMSMessageToObject.class);
        trans2.setReturnDataType(DataTypeFactory.BYTE_ARRAY);
        BytesMessage bMsg = session.createBytesMessage();
        bMsg.writeBytes(compressedBytes);
        Object result = trans2.transform(bMsg);
        assertTrue("Transformed object should be a byte[]", result instanceof byte[]);
        assertTrue("Result should be compressed", compressor.isCompressed((byte[]) result));
        assertTrue("Source and result should be equal", Arrays.equals(compressedBytes, (byte[]) result));
    }

}
