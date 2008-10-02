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

import com.mockobjects.constraint.IsInstanceOf;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.MessageFormatException;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

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
}
