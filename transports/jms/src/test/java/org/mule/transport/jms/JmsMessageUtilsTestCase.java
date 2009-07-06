/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.BananaFactory;
import org.mule.tck.testmodels.fruit.Orange;

import com.mockobjects.constraint.IsInstanceOf;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.io.NotSerializableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageFormatException;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.iterators.IteratorEnumeration;

public class JmsMessageUtilsTestCase extends AbstractMuleTestCase
{
    public static final String ENCODING = "UTF-8";

    public void testHeaders()
    {
        // already valid headers are returned as-is, so we can assertSame
        assertSame("identifier", JmsMessageUtils.encodeHeader("identifier"));
        assertSame("_identifier", JmsMessageUtils.encodeHeader("_identifier"));
        assertSame("identifier_", JmsMessageUtils.encodeHeader("identifier_"));
        assertSame("ident_ifier", JmsMessageUtils.encodeHeader("ident_ifier"));

        assertEquals("_identifier", JmsMessageUtils.encodeHeader("-identifier"));
        assertEquals("identifier_", JmsMessageUtils.encodeHeader("identifier-"));
        assertEquals("ident_ifier", JmsMessageUtils.encodeHeader("ident-ifier"));
        assertEquals("_ident_ifier_", JmsMessageUtils.encodeHeader("-ident_ifier-"));
        assertEquals("_ident_ifier_", JmsMessageUtils.encodeHeader("-ident-ifier-"));
    }

    public void testTextMessageNullContent() throws Exception
    {
        Mock mockMessage = new Mock(TextMessage.class);
        mockMessage.expectAndReturn("getText", null);

        TextMessage mockTextMessage = (TextMessage) mockMessage.proxy();

        byte[] result = JmsMessageUtils.toByteArray(mockTextMessage, JmsConstants.JMS_SPECIFICATION_102B, ENCODING);
        assertNotNull(result);
        assertEquals("Should return an empty byte array.", 0, result.length);

        mockMessage.verify();
    }

    public void testByteMessageNullContent() throws Exception
    {
        // test for JMS 1.0.2-compliant code path
        Mock mockMessage = new Mock(BytesMessage.class);
        mockMessage.expect("reset");
        mockMessage.expectAndReturn("readBytes", new IsInstanceOf(byte[].class), -1);
        BytesMessage mockBytesMessage = (BytesMessage) mockMessage.proxy();

        byte[] result = JmsMessageUtils.toByteArray(mockBytesMessage, JmsConstants.JMS_SPECIFICATION_102B, ENCODING);
        assertNotNull(result);
        assertEquals("Should return an empty byte array.", 0, result.length);
        mockMessage.verify();

        // test for JMS 1.1-compliant code path
        mockMessage = new Mock(BytesMessage.class);
        mockMessage.expect("reset");
        mockMessage.expectAndReturn("getBodyLength", new Long(0));
        mockBytesMessage = (BytesMessage) mockMessage.proxy();

        result = JmsMessageUtils.toByteArray(mockBytesMessage, JmsConstants.JMS_SPECIFICATION_11, ENCODING);
        assertNotNull(result);
        assertEquals("Should return an empty byte array.", 0, result.length);
        mockMessage.verify();
    }

    public void testStreamMessageSerialization() throws Exception
    {
        Session session = null;
        try
        {
            // get a live session
            ConnectionFactory cf = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&broker.useJmx=false");
            session = cf.createConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);

            // create a test list with data
            List data = new ArrayList();
            data.add(new Object());

            // test the invalid input
            try
            {
                JmsMessageUtils.toMessage(data, session);
                fail("Should've failed with MessageFormatException");
            }
            catch (MessageFormatException e)
            {
                // expected
            }


            // test valid types
            data.clear();
            data.add(Boolean.TRUE);
            data.add(new Byte("1"));
            data.add(new Short("2"));
            data.add(new Character('3'));
            data.add(new Integer("4"));
            // can't write Longs: https://issues.apache.org/activemq/browse/AMQ-1965
            //data.add(new Long("5"));
            data.add(new Float("6"));
            data.add(new Double("7"));
            data.add(new String("8"));
            data.add(new byte[] {9, 10});


            StreamMessage result = (StreamMessage) JmsMessageUtils.toMessage(data, session);
            // reset so it's readable
            result.reset();

            assertEquals(Boolean.TRUE, result.readObject());
            assertEquals(new Byte("1"), result.readObject());
            assertEquals(new Short("2"), result.readObject());
            assertEquals(new Character('3'), result.readObject());
            assertEquals(new Integer("4"), result.readObject());
            // can't write Longs: https://issues.apache.org/activemq/browse/AMQ-1965
            //assertEquals(new Long("5"), result.readObject());
            assertEquals(new Float("6"), result.readObject());
            assertEquals(new Double("7"), result.readObject());
            assertEquals(new String("8"), result.readObject());

            assertTrue(Arrays.equals(new byte[] {9, 10}, (byte[]) result.readObject()));


        }
        finally
        {
            if (session != null)
            {
                session.close();
            }
        }
    }

    public void testMapMessageSerialization() throws Exception
    {
        Session session = null;
        try
        {
            // get a live session
            ConnectionFactory cf = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false&broker.useJmx=false");
            session = cf.createConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);

            // create a test Map with data
            Map data = new HashMap();
            data.put("value1", new Float(4));
            data.put("value2", new byte[]{1,2,3});
            data.put("value3", "value3");
            data.put("value4", new Double(67.9));
            data.put("value5", true);

            Message message = JmsMessageUtils.toMessage(data, session);
            assertTrue(message instanceof MapMessage);

            MapMessage mapMessage = (MapMessage)message;
            assertEquals(new Float(4), mapMessage.getFloat("value1"));
            assertTrue(Arrays.equals(new byte[]{1,2,3}, mapMessage.getBytes("value2")));
            assertEquals("value3", mapMessage.getString("value3"));
            assertEquals(new Double(67.9), mapMessage.getDouble("value4"));
            assertTrue(mapMessage.getBoolean("value5"));

            //Lets add a non-primitive object to the map, we should now get and Object message
            data.put("orange", new Orange());

            message = JmsMessageUtils.toMessage(data, session);
            assertTrue(message instanceof ObjectMessage);

            ObjectMessage objectMessage = (ObjectMessage)message;
            Map values = (Map)objectMessage.getObject();

            assertEquals(new Float(4), values.get("value1"));
            assertTrue(Arrays.equals(new byte[]{1,2,3}, (byte[])values.get("value2")));
            assertEquals("value3", values.get("value3"));
            assertEquals(new Double(67.9), values.get("value4"));
            assertTrue((Boolean)values.get("value5"));
            assertEquals(new Orange(), values.get("orange"));

            //Finally lets add an non-serializable object
            data.put("notserializable", new BananaFactory());

            try
            {
                message = JmsMessageUtils.toMessage(data, session);
                fail("attempt to send a non-serializable object in a map should fail");
            }
            catch (Exception e)
            {
                //exprected
                assertTrue(e.getCause() instanceof NotSerializableException);
            }

        }
        finally
        {
            if (session != null)
            {
                session.close();
            }
        }
    }
    
    public void testMapMessageWithNullValue() throws Exception
    {
        String[] keys = new String[] { "key", "null" };
        Iterator<String> keyIterator = IteratorUtils.arrayIterator(keys);
        Enumeration<String> keyEnumeration = new IteratorEnumeration(keyIterator);
        
        Mock mockMessage = new Mock(MapMessage.class);
        mockMessage.expectAndReturn("getMapNames", keyEnumeration);
        mockMessage.expectAndReturn("getObject", C.eq("key"), "value");
        mockMessage.expectAndReturn("getObject", C.eq("null"), null);
        MapMessage message = (MapMessage) mockMessage.proxy();
        
        Object result = JmsMessageUtils.toObject(message, JmsConstants.JMS_SPECIFICATION_11, ENCODING);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        Map map = (Map) result;
        assertEquals("value", map.get("key"));
        assertNull(map.get("null"));

        mockMessage.verify();
    }

}
