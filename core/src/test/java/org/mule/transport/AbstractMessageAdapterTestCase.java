/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.api.transport.MessageAdapter;
import org.mule.tck.AbstractMuleTestCase;

import java.util.Arrays;

public abstract class AbstractMessageAdapterTestCase extends AbstractMuleTestCase
{
    protected void doSetUp() throws Exception
    {
        RequestContext.setEvent(getTestEvent("hello"));
    }

    protected void doTearDown() throws Exception
    {
        RequestContext.clear();
    }

    protected void doTestMessageEqualsPayload(Object payload1, Object payload2) throws Exception
    {
        if (payload1 instanceof byte[] && payload2 instanceof byte[])
        {
            assertTrue(Arrays.equals((byte[]) payload1, (byte[]) payload2));
        }
        else
        {
            assertEquals(payload1, payload2);
        }
    }
    public void testMessageRetrieval() throws Exception
    {
        Object message = getValidMessage();
        MessageAdapter adapter = createAdapter(message);
        MuleMessage muleMessage = new DefaultMuleMessage(adapter);

        doTestMessageEqualsPayload(message, adapter.getPayload());

        byte[] bytes = muleMessage.getPayloadAsBytes();
        assertNotNull(bytes);

        String stringMessage = muleMessage.getPayloadAsString();
        assertNotNull(stringMessage);

        assertNotNull(adapter.getPayload());
    }

    public void testMessageProps() throws Exception
    {
        MessageAdapter adapter = createAdapter(getValidMessage());

        adapter.setProperty("TestString", "Test1");
        adapter.setProperty("TestLong", new Long(20000000));
        adapter.setProperty("TestInt", new Integer(200000));
        assertNotNull(adapter.getPropertyNames());

        Object prop = adapter.getProperty("TestString");
        assertNotNull(prop);
        assertEquals("Test1", prop);

        prop = adapter.getProperty("TestLong");
        assertNotNull(prop);
        assertEquals(new Long(20000000), prop);

        prop = adapter.getProperty("TestInt");
        assertNotNull(prop);
        assertEquals(new Integer(200000), prop);
    }

    public Object getInvalidMessage()
    {
        return new InvalidMessage();
    }

    public abstract Object getValidMessage() throws Exception;

    public abstract MessageAdapter createAdapter(Object payload) throws MessagingException;

    final class InvalidMessage
    {
        public String toString()
        {
            return "invalid message";
        }
    }

}
