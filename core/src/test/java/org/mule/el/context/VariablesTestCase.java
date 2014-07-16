/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.context;

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
    public VariablesTestCase(Variant variant, String mvelOptimizer)
    {
        super(variant, mvelOptimizer);
    }

    @Test
    public void flowVariablesMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setProperty("foo", "bar", PropertyScope.INVOCATION);
        assertTrue(evaluate("flowVars", message) instanceof Map);
    }

    @Test
    public void assignToFlowVariablesMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        assertImmutableVariable("flowVars='foo'", message);
    }

    @Test
    public void flowVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setFlowVariable("foo", "bar");
        assertEquals(event.getFlowVariable("foo"), evaluate("flowVars['foo']", message));
    }

    @Test
    public void assignValueToFlowVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setFlowVariable("foo", "bar_old");
        evaluate("flowVars['foo']='bar'", message);
        assertEquals("bar", event.getFlowVariable("foo"));
    }

    @Test
    public void assignValueToNewFlowVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        evaluate("flowVars['foo']='bar'", message);
        assertEquals("bar", event.getFlowVariable("foo"));
    }

    @Test
    public void sessionVariablesMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, getTestService());
        message.setProperty("foo", "bar", PropertyScope.SESSION);
        assertTrue(evaluate("sessionVars", message) instanceof Map);
    }

    @Test
    public void assignToSessionVariablesMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        assertImmutableVariable("sessionVars='foo'", message);
    }

    @Test
    public void sessionVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setSessionVariable("foo", "bar");
        assertEquals(event.getSessionVariable("foo"), evaluate("sessionVars['foo']", message));
    }

    @Test
    public void assignValueToSessionVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setSessionVariable("foo", "bar_old");
        evaluate("sessionVars['foo']='bar'", message);
        assertEquals("bar", event.getSessionVariable("foo"));
    }

    @Test
    public void assignValueToNewSessionVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        evaluate("sessionVars['foo']='bar'", message);
        assertEquals("bar", event.getSessionVariable("foo"));
    }

    @Test
    public void variableFromFlowScope() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setFlowVariable("foo", "bar");
        event.setSessionVariable("foo", "NOTbar");
        assertEquals(event.getFlowVariable("foo"), evaluate("foo", message));
    }

    @Test
    public void updateVariableFromFlowScope() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setFlowVariable("foo", "bar");
        assertEquals("bar_new", evaluate("foo='bar_new'", message));
    }

    @Test
    public void variableFromSessionScope() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setSessionVariable("foo", "bar");
        assertEquals(event.getSessionVariable("foo"), evaluate("foo", message));
    }

    @Test
    public void updateVariableFromSessionScope() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setSessionVariable("foo", "bar");
        assertEquals("bar_new", evaluate("foo='bar_new'", message));
    }

    @Test
    public void assignValueToVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setFlowVariable("foo", "bar_old");
        evaluate("foo='bar'", message);
        assertEquals("bar", event.getFlowVariable("foo"));
    }

    @Test
    public void assignValueToLocalVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        evaluate("localVar='bar'", message);
    }

    /**
     * See MULE-6381
     */
    @Test
    public void reassignValueToLocalVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        evaluate("localVar='bar';localVar='bar2'", message);
    }

    @Test
    public void localVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        assertEquals("bar", evaluate("localVar='bar';localVar", message));
    }
}
