/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
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
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.Flow;
import org.mule.processor.AsyncInterceptingMessageProcessor;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.SerializationUtils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.SerializationException;
import org.junit.Ignore;
import org.junit.Test;

public class InvocationPropertiesTestCase extends AbstractMuleContextTestCase
{

    /**
     * Session properties set via message API are lost and unavailable via both message and session API's.
     */
    @Test
    @Ignore
    /** Session properties can be set with no event */
    public void setInvocationPropertyNoEvent() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        try
        {
            message.setProperty("key", "value", PropertyScope.INVOCATION);
            fail("IllegalStateException excepted");
        }
        catch (IllegalStateException e)
        {
            // Expected
        }
        catch (Exception e)
        {
            fail("IllegalStateException excepted");

        }

        assertNull(message.getProperty("key", PropertyScope.INVOCATION));

    }

    /**
     * Properties set via via message api are available via session API. But ONLY if RequestContext.setEvent()
     * is used.
     */
    @Test
    @Ignore
    /** Currently no API to get invocation properties from MuleEvent */
    public void setInvocationPropertyOnMessageGetFromEvent() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, getTestService());

        Object nonSerializable = new Object();
        message.setProperty("key", "value", PropertyScope.SESSION);
        message.setProperty("key2", nonSerializable, PropertyScope.SESSION);
        message.setProperty("key3", "ERROR", PropertyScope.INVOCATION);
        message.setProperty("key4", "ERROR", PropertyScope.INBOUND);
        message.setProperty("key5", "ERROR", PropertyScope.OUTBOUND);

        assertEquals(2, event.getSession().getPropertyNamesAsSet().size());
        assertEquals("value", event.getSession().getProperty("key"));
        assertEquals(nonSerializable, event.getSession().getProperty("key2"));
    }

    /**
     * Properties set via via session api are available via message API. But ONLY if RequestContext.setEvent()
     * is used.
     */
    @Test
    @Ignore
    /** Currently no API to set invocation properties on MuleEvent */
    public void setInvocationPropertyOnEventGetFromMessage() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, getTestService());

        Object nonSerializable = new Object();
        event.getSession().setProperty("key", "value");
        message.setProperty("key2", nonSerializable, PropertyScope.SESSION);

        assertEquals(2, message.getPropertyNames(PropertyScope.SESSION).size());
        assertEquals("value", message.getProperty("key", PropertyScope.SESSION));
        assertEquals(nonSerializable, message.getProperty("key2", PropertyScope.SESSION));
        assertNull(message.getProperty("key", PropertyScope.INVOCATION));
        assertNull(message.getProperty("key", PropertyScope.INBOUND));
        assertNull(message.getProperty("key", PropertyScope.OUTBOUND));
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
        assertFalse(asyncEvent.equals(event));
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
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.REQUEST_RESPONSE,
            getTestService());

        SensingNullMessageProcessor flowListener = new SensingNullMessageProcessor();
        Flow flow = new Flow("flow", muleContext);
        flow.setMessageProcessors(Collections.<MessageProcessor> singletonList(flowListener));
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
