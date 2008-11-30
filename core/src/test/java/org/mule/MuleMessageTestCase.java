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

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transport.DefaultMessageAdapter;

import java.util.HashMap;
import java.util.Map;

public class MuleMessageTestCase extends AbstractMuleTestCase
{

    public void testProperties() throws Exception
    {
        //Will be treated as inbound properties
        Map props = new HashMap();
        props.put("inbound-foo", "foo");
        DefaultMessageAdapter adapter = new DefaultMessageAdapter(TEST_MESSAGE, props, null);
        MuleMessage message =  new DefaultMuleMessage(adapter);

        try
        {
            message.setProperty("inbound-bar", "bar", PropertyScope.INBOUND);
            fail("Inboiund scope should be read-only");
        }
        catch (Exception e)
        {
            //Expected
        }

        message.setProperty("invocation-foo", "foo", PropertyScope.INVOCATION);

        //simulate an inbound session
        MuleSession session = getTestSession(getTestService(), muleContext);
        session.setProperty("session-foo", "foo");

        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint("test1", "test://test1?foo=bar&coo=car"), session, true);
        message = event.getMessage();

        try
        {
            message.getPropertyNames(new PropertyScope("XXX", 5));
            fail("Should throw exception, XXX not a valid scope");
        }
        catch (Exception e)
        {
            //Exprected
        }

        assertEquals(0, message.getPropertyNames(PropertyScope.OUTBOUND).size());

        //Endpoint props + any props added to the message
        assertEquals(3, message.getPropertyNames(PropertyScope.INVOCATION).size());

        assertEquals("foo", message.getProperty("invocation-foo"));
        //defined on the endpoint
        assertEquals("bar", message.getProperty("foo"));
        assertEquals("car", message.getProperty("coo"));

        assertEquals("foo", message.getProperty("invocation-foo", PropertyScope.INVOCATION));
        assertNull(message.getProperty("invocation-foo", PropertyScope.INBOUND));
        assertNull(message.getProperty("invocation-foo", PropertyScope.OUTBOUND));

        message.setProperty("outbound-foo", "foo", PropertyScope.OUTBOUND);

        assertEquals("foo", message.getProperty("outbound-foo", PropertyScope.OUTBOUND));
        assertNull(message.getProperty("invocation-foo", PropertyScope.INBOUND));

         //TODO MULE-3999. Should session properties be copied to the message?
//        message.setProperty("session-bar", "bar", PropertyScope.SESSION);
//        assertEquals(2, message.getPropertyNames(PropertyScope.SESSION).size());
//        assertEquals("foo", message.getProperty("session-foo", PropertyScope.SESSION));
//        assertEquals("bar", message.getProperty("session-bar", PropertyScope.SESSION));

        //Session properties are available on the event
        assertEquals("foo", event.getProperty("session-foo"));

    }

    public void testConstructors() throws Exception
    {
        Apple payload = new Apple();
        //Ensure that the MuleMessage is unwrapped correctly
        DefaultMuleMessage message = new DefaultMuleMessage(new DefaultMuleMessage(payload));
        assertEquals(message.getPayload(), payload);

        DefaultMessageAdapter adapter = new DefaultMessageAdapter(payload);

        message = new DefaultMuleMessage(adapter, new HashMap());
        assertEquals(message.getPayload(), payload);
    }
}
