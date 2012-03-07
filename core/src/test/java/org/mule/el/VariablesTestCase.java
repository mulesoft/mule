/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.transport.PropertyScope;

import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

public class VariablesTestCase extends AbstractELTestCase
{
    public VariablesTestCase(Variant variant)
    {
        super(variant);
    }

    @Test
    public void flowVariablesMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setProperty("foo", "bar", PropertyScope.INVOCATION);
        assertTrue(evaluate("flowVariables", message) instanceof Map);
    }

    @Test
    public void assignToFlowVariablesMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        assertImmutableVariable("flowVariables='foo'", message);
    }

    @Test
    public void flowVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setFlowVariable("foo", "bar");
        assertEquals(event.getFlowVariable("foo"), evaluate("flowVariables['foo']", message));
    }

    @Test
    public void assignValueToFlowVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setFlowVariable("foo", "bar_old");
        evaluate("flowVariables['foo']='bar'", message);
        assertEquals("bar", event.getFlowVariable("foo"));
    }

    @Test
    public void assignValueToNewFlowVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        evaluate("flowVariables['foo']='bar'", message);
        assertEquals("bar", event.getFlowVariable("foo"));
    }

    @Test
    public void sessionVariablesMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, getTestService());
        message.setProperty("foo", "bar", PropertyScope.SESSION);
        assertTrue(evaluate("sessionVariables", message) instanceof Map);
    }

    @Test
    public void assignToSessionVariablesMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        assertImmutableVariable("sessionVariables='foo'", message);
    }

    @Test
    public void sessionVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setSessionVariable("foo", "bar");
        assertEquals(event.getSessionVariable("foo"), evaluate("sessionVariables['foo']", message));
    }

    @Test
    public void assignValueToSessionVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setSessionVariable("foo", "bar_old");
        evaluate("sessionVariables['foo']='bar'", message);
        assertEquals("bar", event.getSessionVariable("foo"));
    }

    @Test
    public void assignValueToNewSessionVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        evaluate("sessionVariables['foo']='bar'", message);
        assertEquals("bar", event.getSessionVariable("foo"));
    }

}
