/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.providers;

import org.mule.impl.RequestContext;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.umo.provider.UMOMessageAdapter;

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

    protected void doTestMessageEqualsPayload(Object message, Object payload) throws Exception
    {
        assertEquals(message, payload);
    }

    public void testMessageRetrieval() throws Exception
    {
        Object message = getValidMessage();
        UMOMessageAdapter adapter = createAdapter(message);

        doTestMessageEqualsPayload(message, adapter.getPayload());

        byte[] bytes = adapter.getPayloadAsBytes();
        assertNotNull(bytes);

        String stringMessage = adapter.getPayloadAsString();
        assertNotNull(stringMessage);

        assertNotNull(adapter.getPayload());

        try
        {
            adapter = createAdapter(getInvalidMessage());
            fail("Message adapter should throw MessageTypeNotSupportedException if an invalid message is set");
        }
        catch (MessageTypeNotSupportedException e)
        {
            // expected
        }
    }

    public void testMessageProps() throws Exception
    {
        UMOMessageAdapter adapter = createAdapter(getValidMessage());

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

    public abstract UMOMessageAdapter createAdapter(Object payload) throws MessagingException;

    final class InvalidMessage
    {
        public String toString()
        {
            return "invalid message";
        }
    }

}
