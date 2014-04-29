/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Set;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

public class MessagePropertiesContextTestCase extends AbstractMuleContextTestCase
{
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

        MuleMessage message = e.getMessage();

        assertEquals("Value1", message.getProperty("SESSION_PROP", PropertyScope.SESSION));
        // test case insensitivity
        assertEquals("Value1", message.getProperty("SESSION_prop", PropertyScope.SESSION));
        assertNull(message.getProperty("SESSION_X", PropertyScope.SESSION));
    }

    @Test
    public void testPropertyScopeOrder() throws Exception
    {
        MuleEvent e = getTestEvent("testing");
        e.getSession().setProperty("Prop", "session");

        MuleMessage message = e.getMessage();
        //Note that we cannot write to the Inbound scope, its read only
        message.setProperty("Prop", "invocation", PropertyScope.INVOCATION);
        message.setProperty("Prop", "outbound", PropertyScope.OUTBOUND);

        assertEquals("outbound", message.getProperty("Prop", PropertyScope.OUTBOUND));
        message.removeProperty("Prop", PropertyScope.OUTBOUND);

        assertEquals("invocation", message.getProperty("Prop", PropertyScope.INVOCATION));
        message.removeProperty("Prop", PropertyScope.INVOCATION);

        assertEquals("session", message.getProperty("Prop", PropertyScope.SESSION));
        assertNull(message.getProperty("Prop", PropertyScope.INBOUND));
        assertNull(message.getProperty("Prop", PropertyScope.INVOCATION));
        assertNull(message.getProperty("Prop", PropertyScope.OUTBOUND));
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
    
    @Test
    public void testCopyConstructor() throws Exception
    {
        MessagePropertiesContext mpc = new MessagePropertiesContext();
        mpc.setProperty("FOO", "BAR", PropertyScope.OUTBOUND);
        mpc.setProperty("ABC", "abc", PropertyScope.OUTBOUND);
        mpc.setProperty("DOO", "DAR", PropertyScope.INVOCATION);

        MessagePropertiesContext copy = new MessagePropertiesContext(mpc);
        
        assertSame(mpc.invocationMap, copy.invocationMap);
        assertSame(mpc.sessionMap, copy.sessionMap);
        
        assertNotSame(mpc.inboundMap, copy.inboundMap);
        assertNotSame(mpc.outboundMap, copy.outboundMap);
        doTest(copy);
        
        // Mutate original
        mpc.setProperty("FOO", "OTHER", PropertyScope.OUTBOUND);
        assertSame(copy.getProperty("FOO", PropertyScope.OUTBOUND), "BAR");

        // Mutate copy
        copy.setProperty("ABC", "OTHER", PropertyScope.OUTBOUND);
        assertSame(mpc.getProperty("ABC", PropertyScope.OUTBOUND), "abc");
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

        Set<String> keys = mpc.getPropertyNames(PropertyScope.OUTBOUND);
        assertEquals(2, keys.size());

        for (String key : keys)
        {
            assertTrue(key.equals("FOO") || key.equals("DOO") || key.equals("ABC"));
            assertFalse(key.equals("foo") || key.equals("doo") || key.equals("abc"));
        }
    }
    
    @Test(expected=UnsupportedOperationException.class)
    public void testInboundPropertyNamesImmutable() throws Exception
    {
        MessagePropertiesContext mpc = new MessagePropertiesContext();
        mpc.getPropertyNames(PropertyScope.INBOUND).add("other");
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testOutboundPropertyNamesImmutable() throws Exception
    {
        MessagePropertiesContext mpc = new MessagePropertiesContext();
        mpc.getPropertyNames(PropertyScope.OUTBOUND).add("other");
    }
    @Test(expected=UnsupportedOperationException.class)
    public void testInvocationPropertyNamesImmutable() throws Exception
    {
        MessagePropertiesContext mpc = new MessagePropertiesContext();
        mpc.getPropertyNames(PropertyScope.INVOCATION).add("other");
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testSessionPropertyNamesImmutable() throws Exception
    {
        MessagePropertiesContext mpc = new MessagePropertiesContext();
        mpc.getPropertyNames(PropertyScope.SESSION).add("other");
    }


}
