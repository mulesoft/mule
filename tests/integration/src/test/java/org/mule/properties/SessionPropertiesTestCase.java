/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transport.MessageDispatcher;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.Flow;
import org.mule.transport.vm.VMMessageDispatcher;

import org.junit.Test;

public class SessionPropertiesTestCase extends org.mule.tck.junit4.FunctionalTestCase
{

    @Test
    public void setSessionPropertyUsingAPIGetInFlow() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());

        message.setProperty("key", "value", PropertyScope.SESSION);

        Flow flowA = (Flow) muleContext.getRegistry().lookupFlowConstruct("A");
        MuleEvent result = flowA.process(event);

        assertEquals("value", result.getMessageAsString());
    }

    @Test
    public void setSessionPropertyInFlowGetUsingAPI() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());

        Flow flowA = (Flow) muleContext.getRegistry().lookupFlowConstruct("B");
        MuleEvent result = flowA.process(event);

        assertEquals("value", result.getMessage().getProperty("key", PropertyScope.SESSION));
    }

    @Test
    public void propagateSessionPropertyOverTransportRequestResponse() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());

        Object nonSerializable = new Object();
        message.setProperty("key", "value", PropertyScope.SESSION);
        message.setProperty("keyNonSerializable", nonSerializable, PropertyScope.SESSION);

        Flow flowA = (Flow) muleContext.getRegistry().lookupFlowConstruct(
            "RequestResponseSessionPropertySettingChain");
        MuleEvent result = flowA.process(event);

        assertEquals("value", result.getMessage().getProperty("key", PropertyScope.SESSION));
        assertEquals("value1", result.getMessage().getProperty("key1", PropertyScope.SESSION));
        assertEquals("value2", result.getMessage().getProperty("key2", PropertyScope.SESSION));
        assertEquals("value3", result.getMessage().getProperty("key3", PropertyScope.SESSION));
        assertEquals("value4", result.getMessage().getProperty("key4", PropertyScope.SESSION));
        assertEquals("value5", result.getMessage().getProperty("key5", PropertyScope.SESSION));
        assertEquals(nonSerializable,
            result.getMessage().getProperty("keyNonSerializable", PropertyScope.SESSION));
    }

    @Test
    public void propagateSessionPropertyOverTransportOneWay() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());

        Object nonSerializable = new Object();
        message.setProperty("key", "value", PropertyScope.SESSION);
        message.setProperty("keyNonSerializable", nonSerializable, PropertyScope.SESSION);

        Flow flowA = (Flow) muleContext.getRegistry()
            .lookupFlowConstruct("OneWaySessionPropertySettingChain");
        flowA.process(event);

        MuleMessage out = muleContext.getClient()
            .request("vm://H-out?connector=VMConnector", RECEIVE_TIMEOUT);

        assertNotNull(out);
        assertEquals("value", out.getProperty("key", PropertyScope.SESSION));
        assertEquals("value1", out.getProperty("key1", PropertyScope.SESSION));
        assertEquals("value2", out.getProperty("key2", PropertyScope.SESSION));
        assertNull(out.getProperty("keyNonSerializable", PropertyScope.SESSION));
    }

    @Test
    public void nonSerializableSessionPropertyOneWayFlow() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());

        Object nonSerializable = new Object();
        message.setProperty("keyNonSerializable", nonSerializable, PropertyScope.SESSION);

        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("PassthroughFlow");
        flow.process(event);

        MuleMessage out = muleContext.getClient().request("vm://PassthroughFlow-out?connector=VMConnector",
            RECEIVE_TIMEOUT);

        assertNotNull(out);
        assertNull(out.getProperty("keyNonSerializable", PropertyScope.SESSION));
    }

    /**
     * When invoking a Flow directly session properties are preserved
     */
    @Test
    public void flowRefSessionPropertyPropagation() throws Exception
    {

        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());

        Object nonSerializable = new Object();
        message.setProperty("keyNonSerializable", nonSerializable, PropertyScope.SESSION);
        message.setProperty("key", "value", PropertyScope.SESSION);

        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("FlowRefWithSessionProperties");
        MuleEvent result = flow.process(event);

        assertSame(event.getSession(), result.getSession());

        assertNotNull(result);
        assertEquals("value", result.getMessage().getProperty("key", PropertyScope.SESSION));
        assertEquals("value1", result.getMessage().getProperty("key1", PropertyScope.SESSION));
        assertEquals("value2", result.getMessage().getProperty("key2", PropertyScope.SESSION));
        assertEquals(nonSerializable,
            result.getMessage().getProperty("keyNonSerializable", PropertyScope.SESSION));
    }

    @Test
    public void outboundEndpointSessionMerge() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());
        Object nonSerializable = new Object();
        message.setProperty("keyNonSerializable", nonSerializable, PropertyScope.SESSION);
        message.setProperty("keyNonSerializable2", nonSerializable, PropertyScope.SESSION);
        message.setProperty("key", "value", PropertyScope.SESSION);
        message.setProperty("key2", "value2", PropertyScope.SESSION);

        MessageDispatcher dispatcher = new VMMessageDispatcher(muleContext.getEndpointFactory()
            .getOutboundEndpoint("addSessionPropertiesFlowEndpoint"));
        MuleEvent result = dispatcher.process(event);

        assertNotNull(result);
        assertNotSame(event, result);
        assertEquals("val", result.getSession().getProperty("keyNonSerializable"));
        assertEquals(nonSerializable, result.getSession().getProperty("keyNonSerializable2"));
        assertEquals("value2NEW", result.getSession().getProperty("key2"));
        assertEquals("value3", result.getSession().getProperty("key3"));
        assertNull(result.getMessage().getProperty("nonSerializableBean"));
    }

    @Test
    public void requestReplySessionMerge() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());
        Object nonSerializable = new Object();
        message.setProperty("keyNonSerializable", nonSerializable, PropertyScope.SESSION);
        message.setProperty("keyNonSerializable2", nonSerializable, PropertyScope.SESSION);
        message.setProperty("key", "value", PropertyScope.SESSION);
        message.setProperty("key2", "value2", PropertyScope.SESSION);

        MuleEvent result = ((Flow) muleContext.getRegistry().lookupFlowConstruct("requestResponseFlow")).process(event);

        assertNotNull(result);
        assertNotSame(event, result);
        assertEquals("val", result.getSession().getProperty("keyNonSerializable"));
        assertEquals(nonSerializable, result.getSession().getProperty("keyNonSerializable2"));
        assertEquals("value2NEW", result.getSession().getProperty("key2"));
        assertEquals("value3", result.getSession().getProperty("key3"));
        assertNull(result.getSession().getProperty("nonSerializableBean"));
    }

    @Test
    public void requestReplyNoSessionPropagationSessionMerge() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestSession(
            getTestService(), muleContext));

        Object nonSerializable = new Object();
        event.getSession().setProperty("keyNonSerializable", nonSerializable);
        event.getSession().setProperty("keyNonSerializable2", nonSerializable);
        event.getSession().setProperty("key", "value");
        event.getSession().setProperty("key2", "value2");

        MuleEvent result = ((Flow) muleContext.getRegistry().lookupFlowConstruct(
            "requestResponseNoSessionPropagationFlow")).process(event);

        assertNotNull(result);
        assertNotSame(event, result);
        assertEquals(nonSerializable, result.getSession().getProperty("keyNonSerializable"));
        assertEquals(nonSerializable, result.getSession().getProperty("keyNonSerializable2"));
        assertEquals("value", result.getSession().getProperty("key"));
        assertEquals("value2", result.getSession().getProperty("key2"));
        assertNull(result.getSession().getProperty("nonSerializableBean"));
    }

    @Test
    public void defaultExceptionStrategy() throws Exception
    {
        testFlow("defaultExceptionStrategy");
    }

    @Test
    public void catchExceptionStrategy() throws Exception
    {
        testFlow("catchExceptionStrategy");
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/properties/session-properties-config.xml";
    }
}
