/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.message;

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.MessagePropertiesContext;

import java.util.Set;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

public class MessagePropertiesContextTestCase extends AbstractMuleTestCase
{
    @Override
    public void doTearDown()
    {
        RequestContext.clear();
    }
    
    @Test
    public void testPropertiesCase() throws Exception
    {
        //Default scope
        MessagePropertiesContext mpc = new MessagePropertiesContext();
        mpc.setProperty("FOO", "BAR");
        mpc.setProperty("ABC", "abc");
        mpc.setProperty("DOO", "DAR", PropertyScope.INVOCATION);
        doTest(mpc);
    }

    @Test
    public void testSessionScope() throws Exception
    {
        MuleEvent e = getTestEvent("testing");
        e.getSession().setProperty("SESSION_PROP", "Value1");
        RequestContext.setEvent(e);

        MessagePropertiesContext mpc = new MessagePropertiesContext();

        assertEquals("Value1", mpc.getProperty("SESSION_PROP", PropertyScope.SESSION));
        //test case insensitivity
        assertEquals("Value1", mpc.getProperty("SESSION_prop", PropertyScope.SESSION));
        assertNull(mpc.getProperty("SESSION_X", PropertyScope.SESSION));
    }

    @Test
    public void testPropertyScopeOrder() throws Exception
    {
        MuleEvent e = getTestEvent("testing");
        e.getSession().setProperty("Prop", "session");
        RequestContext.setEvent(e);

        MessagePropertiesContext mpc = new MessagePropertiesContext();
        //Note that we cannot write to the Inbound scope, its read only
        mpc.setProperty("Prop", "invocation", PropertyScope.INVOCATION);
        mpc.setProperty("Prop", "outbound", PropertyScope.OUTBOUND);

        assertEquals("outbound", mpc.getProperty("Prop"));
        mpc.removeProperty("Prop", PropertyScope.OUTBOUND);

        assertEquals("invocation", mpc.getProperty("Prop"));
        mpc.removeProperty("Prop", PropertyScope.INVOCATION);

        assertEquals("session", mpc.getProperty("Prop"));
        assertNull(mpc.getProperty("Prop", PropertyScope.INBOUND));
        assertNull(mpc.getProperty("Prop", PropertyScope.INVOCATION));
        assertNull(mpc.getProperty("Prop", PropertyScope.OUTBOUND));
    }

    @Test
    public void testPropertiesCaseWithMessageCopy() throws Exception
    {
        //Creates a MPC implicitly
        MuleMessage msg = new DefaultMuleMessage("test", muleContext);

        msg.setProperty("FOO", "BAR");
        assertEquals("BAR", msg.getProperty("foo"));

        msg.setProperty("DOO", "DAR", PropertyScope.INVOCATION);

        //Look in all scopes
        assertEquals("DAR", msg.getProperty("doo"));

        //Look in specific scope
        assertEquals("DAR", msg.getProperty("doO", PropertyScope.INVOCATION));

        //Not found using other specific scopes
        assertNull(msg.getProperty("doo", PropertyScope.INBOUND));
        assertNull(msg.getProperty("doo", PropertyScope.OUTBOUND));
        assertNull(msg.getProperty("doo", PropertyScope.SESSION));

        //This will invoke the copy method on the MPC, want to make sure the copy function behaves as expected
        MuleMessage copy = new DefaultMuleMessage("test copy", msg, muleContext);

        assertEquals("BAR", copy.getProperty("foo"));

        //Look in all scopes
        assertEquals("DAR", copy.getProperty("doo"));

        //Look in specific scope
        assertEquals("DAR", copy.getProperty("doO", PropertyScope.INVOCATION));

        //Not found using other specific scopes
        assertNull(copy.getProperty("doo", PropertyScope.INBOUND));
        assertNull(copy.getProperty("doo", PropertyScope.OUTBOUND));
        assertNull(copy.getProperty("doo", PropertyScope.SESSION));
    }

    @Test
    public void testPropertiesCaseAfterSerialization() throws Exception
    {
        //Default scope
        MessagePropertiesContext mpc = new MessagePropertiesContext();
        mpc.setProperty("FOO", "BAR");
        mpc.setProperty("ABC", "abc");
        mpc.setProperty("DOO", "DAR", PropertyScope.INVOCATION);
        doTest(mpc);

        //Serialize and deserialize
        byte[] bytes = SerializationUtils.serialize(mpc);
        mpc = (MessagePropertiesContext) SerializationUtils.deserialize(bytes);
        doTest(mpc);
    }

    protected void doTest(MessagePropertiesContext mpc)
    {
        //Look in all scopes
        assertEquals("BAR", mpc.getProperty("foo"));
        assertEquals("DAR", mpc.getProperty("doo"));
        assertEquals("abc", mpc.getProperty("abc"));

        //Look in specific scope
        assertEquals("BAR", mpc.getProperty("foO", PropertyScope.OUTBOUND)); //default scope
        assertEquals("DAR", mpc.getProperty("doO", PropertyScope.INVOCATION));

        //Not found using other specific scopes
        assertNull(mpc.getProperty("doo", PropertyScope.INBOUND));
        assertNull(mpc.getProperty("doo", PropertyScope.OUTBOUND));
        assertNull(mpc.getProperty("doo", PropertyScope.SESSION));

        Set<String> keys = mpc.getPropertyNames();
        assertEquals(3, keys.size());

        for (String key : keys)
        {
            assertTrue(key.equals("FOO") || key.equals("DOO") || key.equals("ABC"));
            assertFalse(key.equals("foo") || key.equals("doo") || key.equals("abc"));
        }
    }
}
