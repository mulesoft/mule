/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.SimpleFlowConstruct;
import org.mule.processor.AsyncInterceptingMessageProcessor;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.SerializationUtils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.SerializationException;
import org.junit.Test;

public class InvocationPropertiesTestCase extends AbstractMuleContextTestCase
{

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
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestSession(
            getTestService(), muleContext));

        message.setInvocationProperty("key", "value");

        async.process(event);
        asyncListener.latch.await(RECEIVE_TIMEOUT,
            edu.emory.mathcs.backport.java.util.concurrent.TimeUnit.MILLISECONDS);

        MuleEvent asyncEvent = asyncListener.event;

        // Event is copied, but session isn't
        assertNotSame(asyncEvent, event);
        assertFalse(asyncEvent.equals(event));
        assertSame(asyncEvent.getSession(), event.getSession());

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
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestSession(
            getTestService(), muleContext));

        message.setInvocationProperty("key", "value");

        MuleEvent deserializedEvent = (MuleEvent) SerializationUtils.deserialize(
            SerializationUtils.serialize(event), muleContext);

        // Event and session are both copied
        assertNotSame(deserializedEvent, event);
        assertFalse(deserializedEvent.equals(event));
        assertNotSame(deserializedEvent.getSession(), event.getSession());
        assertFalse(deserializedEvent.getSession().equals(event.getSession()));

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
     * Serialization of a MuleSession with session properties serializes only
     * serializable properties
     */
    @Test
    public void serializationNonSerializableInvocationPropertyPropagation() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestSession(
            getTestService(), muleContext));

        Object nonSerializable = new Object();
        message.setInvocationProperty("key", nonSerializable);
        message.setInvocationProperty("key2", "value2");

        try
        {
            SerializationUtils.deserialize(SerializationUtils.serialize(event), muleContext);
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
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestSession(
            getTestService(), muleContext));

        SensingNullMessageProcessor flowListener = new SensingNullMessageProcessor();
        List<MessageProcessor> processors = new ArrayList<MessageProcessor>();
        processors.add(new MessageProcessor()
        {
            
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setInvocationProperty("newKey", "newValue");
                return event;
            }
        });
        processors.add(flowListener);
        
        SimpleFlowConstruct flow = new SimpleFlowConstruct("flow", muleContext);
        flow.setMessageProcessors(processors);
        flow.initialise();
        flow.start();

        Object nonSerializable = new Object();
        message.setInvocationProperty("key", "value");
        message.setInvocationProperty("key2", nonSerializable);

        flow.process(event);

        flowListener.latch.await(RECEIVE_TIMEOUT,
            edu.emory.mathcs.backport.java.util.concurrent.TimeUnit.MILLISECONDS);
        MuleEvent processedEvent = flowListener.event;

        // Event is copied, but session isn't
        assertNotSame(processedEvent, event);
        assertEquals(processedEvent, event);
        assertNotSame(processedEvent.getSession(), event.getSession());

        // Session properties available before new flow are available after too
        assertEquals("value", processedEvent.getMessage().getInvocationProperty("key"));
        assertEquals(nonSerializable, processedEvent.getMessage().getInvocationProperty("key2"));

        // Session properties set after new flow are available in message processor
        // before new flow
        assertEquals("newValue", event.getMessage().getInvocationProperty("newKey"));
        assertEquals("newValue", processedEvent.getMessage().getInvocationProperty("newKey"));

        flow.stop();
        flow.dispose();
    }

}
