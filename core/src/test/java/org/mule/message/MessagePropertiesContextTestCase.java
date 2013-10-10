/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.message;

import org.mule.MessagePropertiesContext;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Set;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MessagePropertiesContextTestCase extends AbstractMuleContextTestCase
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
        mpc.setProperty("FOO", "BAR", PropertyScope.OUTBOUND);
        mpc.setProperty("ABC", "abc", PropertyScope.OUTBOUND);
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

        assertEquals("outbound", mpc.getProperty("Prop", PropertyScope.OUTBOUND));
        mpc.removeProperty("Prop", PropertyScope.OUTBOUND);

        assertEquals("invocation", mpc.getProperty("Prop", PropertyScope.INVOCATION));
        mpc.removeProperty("Prop", PropertyScope.INVOCATION);

        assertEquals("session", mpc.getProperty("Prop", PropertyScope.SESSION));
        assertNull(mpc.getProperty("Prop", PropertyScope.INBOUND));
        assertNull(mpc.getProperty("Prop", PropertyScope.INVOCATION));
        assertNull(mpc.getProperty("Prop", PropertyScope.OUTBOUND));
    }

    @Test
    public void testPropertiesCaseAfterSerialization() throws Exception
    {
        //Default scope
        MessagePropertiesContext mpc = new MessagePropertiesContext();
        mpc.setProperty("FOO", "BAR", PropertyScope.OUTBOUND);
        mpc.setProperty("ABC", "abc", PropertyScope.OUTBOUND);
        mpc.setProperty("DOO", "DAR", PropertyScope.INVOCATION);
        doTest(mpc);

        //Serialize and deserialize
        byte[] bytes = SerializationUtils.serialize(mpc);
        mpc = (MessagePropertiesContext) SerializationUtils.deserialize(bytes);
        doTest(mpc);
    }

    /*@Test
    public void testInboundScopeIsImmutable() throws Exception
    {        
        MessagePropertiesContext mpc = new MessagePropertiesContext();
        try
        {
            mpc.setProperty("key", "value", PropertyScope.INBOUND);
            fail("Inbound scope should be read-only");
        }
        catch (IllegalArgumentException iae)
        {
            // this exception was expected
        }
    }*/
        
    protected void doTest(MessagePropertiesContext mpc)
    {
        //Look in all scopes
        assertEquals("BAR", mpc.getProperty("foo", PropertyScope.OUTBOUND));
        assertEquals("DAR", mpc.getProperty("doo", PropertyScope.INVOCATION));
        assertEquals("abc", mpc.getProperty("abc", PropertyScope.OUTBOUND));

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
