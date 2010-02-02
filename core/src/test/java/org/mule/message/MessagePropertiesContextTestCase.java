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

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.MessagePropertiesContext;

public class MessagePropertiesContextTestCase extends AbstractMuleTestCase
{
    public void testPropertiesCase() throws Exception
    {
        //Default scope
        MessagePropertiesContext mpc = new MessagePropertiesContext();
        mpc.setProperty("FOO", "BAR");
        assertEquals(mpc.getProperty("foo"), "BAR");


        mpc.setProperty("DOO", "DAR", PropertyScope.INVOCATION);

        //Look in all scopes
        assertEquals(mpc.getProperty("doo"), "DAR");

        //Look in specific scope
        assertEquals(mpc.getProperty("doO", PropertyScope.INVOCATION), "DAR");

        //Not found using other specific scopes
        assertNull(mpc.getProperty("doo", PropertyScope.INBOUND));
        assertNull(mpc.getProperty("doo", PropertyScope.OUTBOUND));
        assertNull(mpc.getProperty("doo", PropertyScope.SESSION));
    }

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
}
