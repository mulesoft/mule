/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.serialization.ObjectSerializer;
import org.mule.api.serialization.SerializationException;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.Flow;
import org.mule.processor.AsyncInterceptingMessageProcessor;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class InvocationPropertiesTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void setInvocationPropertyBeforeNewEvent() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        message.setProperty("key", "value", PropertyScope.INVOCATION);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, getTestService());
        assertEquals("value", message.getProperty("key", PropertyScope.INVOCATION));
        assertEquals("value", event.getFlowVariable("key"));
    }

    @Test
    public void setInvocationPropertyBeforeExistingEvent() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        message.setProperty("key", "value", PropertyScope.INVOCATION);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, getTestService());
        event.setFlowVariable("key", "VALUE");
        event.setFlowVariable("key2", "value2");
        assertEquals("VALUE", message.getProperty("key", PropertyScope.INVOCATION));
        assertEquals("value2", message.getProperty("key2", PropertyScope.INVOCATION));
        assertEquals("VALUE", event.getFlowVariable("key"));
        assertEquals("value2", event.getFlowVariable("key2"));
    }

    @Test
    public void setInvocationPropertyViaMessageAPI() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, getTestService());

        Object nonSerializable = new Object();
        message.setProperty("key", "value", PropertyScope.INVOCATION);
        message.setProperty("key2", nonSerializable, PropertyScope.INVOCATION);

        assertEquals(2, event.getFlowVariableNames().size());
        assertEquals("value", event.getFlowVariable("key"));
        assertEquals(nonSerializable, event.getFlowVariable("key2"));
    }

    @Test
    public void getInvocationPropertyViaMessageAPI() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, getTestService());

        Object nonSerializable = new Object();
        event.setFlowVariable("key", "value");
        event.setFlowVariable("key2", nonSerializable);

        assertEquals(2, message.getPropertyNames(PropertyScope.INVOCATION).size());
        assertEquals("value", message.getInvocationProperty("key"));
        assertEquals(nonSerializable, message.getInvocationProperty("key2"));
    }

    /**
     * MuleSession is not copied when async intercepting processor is used
     */
    @Test
    public void asyncInterceptingProcessorInvocationPropertyPropagation() throws Exception
    {
        AsyncInterceptingMessageProcessor async = new AsyncInterceptingMessageProcessor(
                muleContext.getDefaultThreadingProfile(), "async", 0);
        SensingNullMessageProcessor asyncListener = new SensingNullMessageProcessor();
        async.setListener(asyncListener);
        async.start();

        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, getTestService());

        message.setInvocationProperty("key", "value");

        async.process(event);
        asyncListener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);

        MuleEvent asyncEvent = asyncListener.event;

        // Event is copied, but session isn't
        assertNotSame(asyncEvent, event);
        assertNotSame(asyncEvent.getSession(), event.getSession());

        // Session properties available before async are available after too
        assertEquals("value", message.getInvocationProperty("key"));

        // Session properties set after async are available in message processor
        // before async
        message.setProperty("newKey", "newValue", PropertyScope.INVOCATION);
        assertEquals("newValue", message.getInvocationProperty("newKey"));
        assertNull(event.getSession().getProperty("newKey"));

        async.stop();
    }

    /**
     * MuleSession is not copied when async intercepting processor is used
     */
    @Test
    public void serializationInvocationPropertyPropagation() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, getTestService());

        message.setInvocationProperty("key", "value");

        ObjectSerializer serializer = muleContext.getObjectSerializer();
        MuleEvent deserializedEvent = serializer.deserialize(serializer.serialize(event));

        // Event and session are both copied
        assertNotSame(deserializedEvent, event);
        assertNotSame(deserializedEvent, event);
        assertNotSame(deserializedEvent.getSession(), event.getSession());
        assertNotSame(deserializedEvent.getSession(), event.getSession());

        // Session properties available before serialization are available after too
        assertEquals("value", deserializedEvent.getMessage().getInvocationProperty("key"));

        // Session properties set after deserialization are not available in message
        // processor
        // before serialization
        deserializedEvent.getMessage().setInvocationProperty("newKey", "newValue");
        assertEquals("newValue", deserializedEvent.getMessage().getInvocationProperty("newKey"));
        assertNull(event.getMessage().getInvocationProperty("newKey"));
    }

    /**
     * Serialization of a MuleSession with session properties serializes only serializable properties
     */
    @Test
    public void serializationNonSerializableInvocationPropertyPropagation() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, getTestService());

        Object nonSerializable = new Object();
        message.setInvocationProperty("key", nonSerializable);
        message.setInvocationProperty("key2", "value2");

        ObjectSerializer serializer = muleContext.getObjectSerializer();

        try
        {
            serializer.deserialize(serializer.serialize(event));
            fail("Serialization should have failed.");
        }
        catch (Exception e)
        {
            assertEquals(SerializationException.class, e.getClass());
        }
    }

    /**
     * When invoking a Flow directly session properties are preserved
     */
    @Test
    public void processFlowInvocationPropertyPropagation() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.REQUEST_RESPONSE,
                                               getTestService());

        SensingNullMessageProcessor flowListener = new SensingNullMessageProcessor();
        Flow flow = new Flow("flow", muleContext);
        flow.setMessageProcessors(Collections.<MessageProcessor>singletonList(flowListener));
        flow.initialise();
        flow.start();

        Object nonSerializable = new Object();
        message.setInvocationProperty("key", "value");
        message.setInvocationProperty("key2", nonSerializable);

        flow.process(event);

        flowListener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);
        MuleEvent processedEvent = flowListener.event;

        // Event is copied, but session isn't
        assertNotSame(processedEvent, event);
        assertEquals(processedEvent, event);
        assertSame(processedEvent.getSession(), event.getSession());

        // Session properties available before new flow are available after too
        assertEquals("value", processedEvent.getMessage().getInvocationProperty("key"));
        assertEquals(nonSerializable, processedEvent.getMessage().getInvocationProperty("key2"));

        // Session properties set after new flow are available in message processor
        // before new flow
        processedEvent.getMessage().setInvocationProperty("newKey", "newValue");
        assertEquals("newValue", processedEvent.getMessage().getInvocationProperty("newKey"));
        assertEquals("newValue", event.getMessage().getInvocationProperty("newKey"));

        flow.stop();
        flow.dispose();
    }

}
