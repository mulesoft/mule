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
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.PropertyScope;
import org.mule.processor.AsyncInterceptingMessageProcessor;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.SerializationUtils;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class SessionPropertiesTestCase extends AbstractMuleContextTestCase
{

    /**
     * Session properties set via message API are lost and unavailable via both
     * message and session API's.
     */
    @Test
    public void setSessionPropertyNoEvent() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        message.setProperty("key", "value", PropertyScope.SESSION);

        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestSession(
            getTestService(), muleContext));

        RequestContext.setEvent(event);

        assertNull(message.getProperty("key", PropertyScope.SESSION));
        assertNull(event.getSession().getProperty("key"));

    }

    /**
     * Properties set via via message api are available via session API. But ONLY if
     * RequestContext.setEvent() is used.
     */
    @Test
    public void setSessionPropertyOnMessageGetFromSession() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestSession(
            getTestService(), muleContext));

        RequestContext.setEvent(event);

        message.setProperty("key", "value", PropertyScope.SESSION);
        message.setProperty("key2", "ERROR", PropertyScope.INVOCATION);
        message.setProperty("key3", "ERROR", PropertyScope.INBOUND);
        message.setProperty("key4", "ERROR", PropertyScope.OUTBOUND);

        assertEquals(1, event.getSession().getPropertyNamesAsSet().size());
        assertEquals("value", event.getSession().getProperty("key"));
    }

    /**
     * Properties set via via session api are available via message API. But ONLY if
     * RequestContext.setEvent() is used.
     */
    @Test
    public void setSessionPropertyOnSessionGetFromMessage() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestSession(
            getTestService(), muleContext));

        RequestContext.setEvent(event);

        event.getSession().setProperty("key", "value");

        assertEquals(1, message.getPropertyNames(PropertyScope.SESSION).size());
        assertEquals("value", message.getProperty("key", PropertyScope.SESSION));
        assertNull(message.getProperty("key", PropertyScope.INVOCATION));
        assertNull(message.getProperty("key", PropertyScope.INBOUND));
        assertNull(message.getProperty("key", PropertyScope.OUTBOUND));
    }

    /**
     * MuleSession is not copied when async intercepting processor is used
     */
    @Test
    public void asyncInterceptingProcessorMuleSession() throws Exception
    {
        AsyncInterceptingMessageProcessor async = new AsyncInterceptingMessageProcessor(
            muleContext.getDefaultThreadingProfile(), "async", 0);
        SensingNullMessageProcessor asyncListener = new SensingNullMessageProcessor();
        async.setListener(asyncListener);
        async.start();

        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestSession(
            getTestService(), muleContext));

        event.getSession().setProperty("key", "value");

        async.process(event);
        asyncListener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);

        MuleEvent asyncEvent = asyncListener.event;

        // Event is copied, but session isn't
        assertNotSame(asyncEvent, event);
        assertFalse(asyncEvent.equals(event));
        assertSame(asyncEvent.getSession(), event.getSession());

        // Session properties available before async are available after too
        assertEquals(1, asyncEvent.getSession().getPropertyNamesAsSet().size());
        assertEquals("value", asyncEvent.getSession().getProperty("key"));

        // Session properties set after async are available in message processor
        // before async
        asyncEvent.getSession().setProperty("newKey", "newValue");
        assertEquals(2, asyncEvent.getSession().getPropertyNamesAsSet().size());
        assertEquals("newValue", asyncEvent.getSession().getProperty("newKey"));
        assertEquals(2, event.getSession().getPropertyNamesAsSet().size());
        assertEquals("newValue", event.getSession().getProperty("newKey"));

        async.stop();
    }

    /**
     * MuleSession is not copied when async intercepting processor is used
     */
    @Test
    public void serializedMuleSession() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestSession(
            getTestService(), muleContext));

        event.getSession().setProperty("key", "value");

        MuleEvent deserializedEvent = (MuleEvent) SerializationUtils.deserialize(
            SerializationUtils.serialize(event), muleContext);

        // Event and session are both copied
        assertNotSame(deserializedEvent, event);
        assertFalse(deserializedEvent.equals(event));
        assertNotSame(deserializedEvent.getSession(), event.getSession());
        assertFalse(deserializedEvent.getSession().equals(event.getSession()));

        // Session properties available before serialization are available after too
        assertEquals(1, deserializedEvent.getSession().getPropertyNamesAsSet().size());
        assertEquals("value", deserializedEvent.getSession().getProperty("key"));

        // Session properties set after deserialization are not available in message
        // processor
        // before serialization
        deserializedEvent.getSession().setProperty("newKey", "newValue");
        assertEquals(2, deserializedEvent.getSession().getPropertyNamesAsSet().size());
        assertEquals("newValue", deserializedEvent.getSession().getProperty("newKey"));
        assertEquals(1, event.getSession().getPropertyNamesAsSet().size());
        assertNull(event.getSession().getProperty("newKey"));
    }

    /**
     * Serialization of a MuleSession with session properties fails (no warning is
     * given)
     */
    @Test
    public void defaultSessionHandlerPropertyPropagation() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestSession(
            getTestService(), muleContext));

        event.getSession().setProperty("key", "value");

        // Serialize and deserialize session using default session handler
        new SerializeAndEncodeSessionHandler().storeSessionInfoToMessage(event.getSession(), message);
        message.setProperty(MuleProperties.MULE_SESSION_PROPERTY,
            message.getProperty(MuleProperties.MULE_SESSION_PROPERTY, PropertyScope.OUTBOUND),
            PropertyScope.INBOUND);
        MuleSession newSession = new SerializeAndEncodeSessionHandler().retrieveSessionInfoFromMessage(message);

        // Session after deserialization is a new instance that does not equal old
        // instance
        assertNotSame(newSession, event.getSession());
        assertFalse(newSession.equals(event.getSession()));

        // Session properties available before serialization are available after too
        assertEquals(1, newSession.getPropertyNamesAsSet().size());
        assertEquals("value", newSession.getProperty("key"));

        // Session properties set after deserialization are not available in message
        // processor
        // before serialization
        newSession.setProperty("newKey", "newValue");
        assertEquals(2, newSession.getPropertyNamesAsSet().size());
        assertEquals("newValue", newSession.getProperty("newKey"));
        assertEquals(1, event.getSession().getPropertyNamesAsSet().size());
        assertNull(event.getSession().getProperty("newKey"));
    }

    /**
     * Serialization of a MuleSession with session properties fails (no warning is
     * given)
     */
    @Test
    public void nonSerializableSessionPropertiesSerialization() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestSession(
            getTestService(), muleContext));

        Object obj1 = new Object();

        event.getSession().setProperty("key", obj1);

        try
        {
            SerializationUtils.deserialize(SerializationUtils.serialize(event), muleContext);
            fail("Exception expected");
        }
        catch (Exception e)
        {

        }
    }

    /**
     * Serialization of a MuleSession with session properties to message using
     * SessionHandler serializes only serializable properties
     */
    @Test
    public void nonSerializableSessionPropertiesDefaultSessionHandler() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        MuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestSession(
            getTestService(), muleContext));

        Object obj1 = new Object();

        event.getSession().setProperty("key", obj1);

        // Serialize and deserialize session using default session handler
        new SerializeAndEncodeSessionHandler().storeSessionInfoToMessage(event.getSession(), message);
        message.setProperty(MuleProperties.MULE_SESSION_PROPERTY,
            message.getProperty(MuleProperties.MULE_SESSION_PROPERTY, PropertyScope.OUTBOUND),
            PropertyScope.INBOUND);
        MuleSession newSession = new SerializeAndEncodeSessionHandler().retrieveSessionInfoFromMessage(message);

        // Session after deserialization is a new instance that does not equal old
        // instance
        assertNotSame(newSession, event.getSession());
        assertFalse(newSession.equals(event.getSession()));

        // Non-serilizable session properties available before serialization are not
        // available after too
        assertEquals(0, newSession.getPropertyNamesAsSet().size());
        assertNull(newSession.getProperty("key"));
    }

}
