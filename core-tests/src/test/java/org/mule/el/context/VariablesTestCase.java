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
import org.mule.PropertyScope;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class VariablesTestCase extends AbstractELTestCase
{
    private MuleEvent event;

    public VariablesTestCase(Variant variant, String mvelOptimizer)
    {
        super(variant, mvelOptimizer);
    }

    @Before
    public void setup() throws Exception
    {
        event = getTestEvent("");
    }

    @Test
    public void flowVariablesMap() throws Exception
    {
        event.getMessage().setProperty("foo", "bar", PropertyScope.INVOCATION);
        assertTrue(evaluate("flowVars", event) instanceof Map);
    }

    @Test
    public void assignToFlowVariablesMap() throws Exception
    {
        assertImmutableVariable("flowVars='foo'", event);
    }

    @Test
    public void flowVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setFlowVariable("foo", "bar");
        assertEquals(event.getFlowVariable("foo"), evaluate("flowVars['foo']", event));
    }

    @Test
    public void assignValueToFlowVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setFlowVariable("foo", "bar_old");
        evaluate("flowVars['foo']='bar'", event);
        assertEquals("bar", event.getFlowVariable("foo"));
    }

    @Test
    public void assignValueToNewFlowVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        evaluate("flowVars['foo']='bar'", event);
        assertEquals("bar", event.getFlowVariable("foo"));
    }

    @Test
    public void sessionVariablesMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, getTestFlow());
        message.setProperty("foo", "bar", PropertyScope.SESSION);
        assertTrue(evaluate("sessionVars", event) instanceof Map);
    }

    @Test
    public void assignToSessionVariablesMap() throws Exception
    {
        assertImmutableVariable("sessionVars='foo'", event);
    }

    @Test
    public void sessionVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.getSession().setProperty("foo", "bar");
        assertEquals(event.getSession().getProperty("foo"), evaluate("sessionVars['foo']", event));
    }

    @Test
    public void assignValueToSessionVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.getSession().setProperty("foo", "bar_old");
        evaluate("sessionVars['foo']='bar'", event);
        assertEquals("bar", event.getSession().getProperty("foo"));
    }

    @Test
    public void assignValueToNewSessionVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        evaluate("sessionVars['foo']='bar'", event);
        assertEquals("bar", event.getSession().getProperty("foo"));
    }

    @Test
    public void variableFromFlowScope() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setFlowVariable("foo", "bar");
        event.getSession().setProperty("foo", "NOTbar");
        assertEquals(event.getFlowVariable("foo"), evaluate("foo", event));
    }

    @Test
    public void updateVariableFromFlowScope() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setFlowVariable("foo", "bar");
        assertEquals("bar_new", evaluate("foo='bar_new'", event));
    }

    @Test
    public void variableFromSessionScope() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.getSession().setProperty("foo", "bar");
        assertEquals(event.getSession().getProperty("foo"), evaluate("foo", event));
    }

    @Test
    public void updateVariableFromSessionScope() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.getSession().setProperty("foo", "bar");
        assertEquals("bar_new", evaluate("foo='bar_new'", event));
    }

    @Test
    public void assignValueToVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        event.setFlowVariable("foo", "bar_old");
        evaluate("foo='bar'", event);
        assertEquals("bar", event.getFlowVariable("foo"));
    }

    @Test
    public void assignValueToLocalVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        evaluate("localVar='bar'", event);
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
        evaluate("localVar='bar';localVar='bar2'", event);
    }

    @Test
    public void localVariable() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY,
            Mockito.mock(FlowConstruct.class));
        assertEquals("bar", evaluate("localVar='bar';localVar", event));
    }
}
