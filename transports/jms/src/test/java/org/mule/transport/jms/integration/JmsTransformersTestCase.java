/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.integration;

import org.mule.RequestContext;
import org.mule.transport.jms.JmsConnector;
import org.mule.transport.jms.transformers.AbstractJmsTransformer;
import org.mule.transport.jms.transformers.JMSMessageToObject;
import org.mule.transport.jms.transformers.ObjectToJMSMessage;
import org.mule.util.FileUtils;
import org.mule.util.compression.CompressionStrategy;
import org.mule.util.compression.GZipCompression;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
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

    public JmsTransformersTestCase(JmsVendorConfiguration config)
    {
        super(config);
    }

    protected String getConfigResources()
    {
        return "integration/jms-transformers.xml";
    }

    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        JmsConnector connector = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnector1");
        ConnectionFactory cf = connector.getConnectionFactory();

        session = cf.createConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    // @Override
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
        AbstractJmsTransformer trans = new JMSMessageToObject();
        Object result = trans.transform(oMsg);
        assertTrue("Transformed object should be a File", result.getClass().equals(File.class));

        AbstractJmsTransformer trans2 = new SessionEnabledObjectToJMSMessage(session);
        trans2.setReturnClass(ObjectMessage.class);
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

        AbstractJmsTransformer trans = new JMSMessageToObject();
        Object result = trans.transform(tMsg);
        assertTrue("Transformed object should be a string", text.equals(result.toString()));

        AbstractJmsTransformer trans2 = new SessionEnabledObjectToJMSMessage(session);
        trans2.setReturnClass(TextMessage.class);
        Object result2 = trans2.transform(text);
        assertTrue("Transformed object should be a TextMessage", result2 instanceof TextMessage);
    }

    @Test
    public void testTransformMapMessage() throws Exception
    {
        RequestContext.setEvent(getTestEvent("test"));

        Properties p = new Properties();
        p.setProperty("Key1", "Value1");
        p.setProperty("Key2", "Value2");
        p.setProperty("Key3", "Value3");

        AbstractJmsTransformer trans = new SessionEnabledObjectToJMSMessage(session);
        trans.setReturnClass(MapMessage.class);
        Object result2 = trans.transform(p);
        assertTrue("Transformed object should be a MapMessage", result2 instanceof MapMessage);

        MapMessage mMsg = (MapMessage) result2;
        AbstractJmsTransformer trans2 = new JMSMessageToObject();
        trans2.setReturnClass(Map.class);
        Object result = trans2.transform(mMsg);
        assertTrue("Transformed object should be a Map", result instanceof Map);
    }

    @Test
    public void testTransformByteMessage() throws Exception
    {
        RequestContext.setEvent(getTestEvent("test"));

        AbstractJmsTransformer trans = new SessionEnabledObjectToJMSMessage(session);
        trans.setReturnClass(BytesMessage.class);
        String text = "This is a test BytesMessage";
        Object result2 = trans.transform(text.getBytes());
        assertTrue("Transformed object should be a BytesMessage", result2 instanceof BytesMessage);

        AbstractJmsTransformer trans2 = new JMSMessageToObject();
        trans2.setReturnClass(byte[].class);
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
        List list = new ArrayList();
        list.add(new Integer(i));
        list.add(new Double(d));
        list.add(text);

        StreamMessage message = session.createStreamMessage();
        message.writeString(text);
        message.writeInt(i);
        message.writeDouble(d);
        message.reset();

        AbstractJmsTransformer trans = new JMSMessageToObject();
        Object transformedObject = trans.transform(message);
        assertTrue("Transformed object should be a List", transformedObject instanceof List);

        final List result = (List) transformedObject;
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
        trans.setReturnClass(BytesMessage.class);
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
        AbstractJmsTransformer trans2 = new JMSMessageToObject();
        trans2.setReturnClass(byte[].class);
        BytesMessage bMsg = session.createBytesMessage();
        bMsg.writeBytes(compressedBytes);
        Object result = trans2.transform(bMsg);
        assertTrue("Transformed object should be a byte[]", result instanceof byte[]);
        assertTrue("Result should be compressed", compressor.isCompressed((byte[]) result));
        assertTrue("Source and result should be equal", Arrays.equals(compressedBytes, (byte[]) result));
    }

    /*
     * This class overrides getSession() to return the specified test MuleSession;
     * otherwise we would need a full-fledged JMS connector with dispatchers etc.
     * TODO check if we really need this stateful transformer now
     */
    public static class SessionEnabledObjectToJMSMessage extends ObjectToJMSMessage
    {
        private final Session transformerSession;

        public SessionEnabledObjectToJMSMessage(Session session)
        {
            super();
            transformerSession = session;
        }

        // @Override
        protected Session getSession()
        {
            return transformerSession;
        }
    }

}
