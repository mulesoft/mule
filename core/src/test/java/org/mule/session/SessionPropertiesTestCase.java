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
import static org.mule.session.AbstractSessionHandler.ACTIVATE_NATIVE_SESSION_SERIALIZATION_PROPERTY;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.serialization.ObjectSerializer;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.Flow;
import org.mule.processor.AsyncInterceptingMessageProcessor;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

@Ignore("This test has to be run alone, because the system property it sets invalidates the run of SerializeAndEncodeSessionHandlerTestCase afterwards")
public class SessionPropertiesTestCase extends AbstractMuleContextTestCase
{

    @ClassRule
    public static SystemProperty sessionSignKey = new SystemProperty(ACTIVATE_NATIVE_SESSION_SERIALIZATION_PROPERTY, "true");

    private DefaultMuleMessage message;

    @Before
    public void setup()
    {
        message = new DefaultMuleMessage("data", muleContext);
        message.setInboundProperty("MULE_ENDPOINT", "http:whatever");
    }

    /**
     * Session properties set via message API are lost and unavailable via both message and session API's.
     */
    @Test
    public void setSessionPropertyNoEvent() throws Exception
    {
        try
        {
            message.setProperty("key", "value", PropertyScope.SESSION);
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

        assertNull(message.getProperty("key", PropertyScope.SESSION));

    }

    /**
     * Properties set via via message api are available via session API. But ONLY if RequestContext.setEvent()
     * is used.
     */
    @Test
    public void setSessionPropertyOnMessageGetFromSession() throws Exception
    {
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
    public void setSessionPropertyOnSessionGetFromMessage() throws Exception
    {
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
    public void asyncInterceptingProcessorSessionPropertyPropagation() throws Exception
    {
        AsyncInterceptingMessageProcessor async = new AsyncInterceptingMessageProcessor(
            muleContext.getDefaultThreadingProfile(), "async", 0);
        SensingNullMessageProcessor asyncListener = new SensingNullMessageProcessor();
        async.setListener(asyncListener);
        async.start();

        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, getTestService());

        event.getSession().setProperty("key", "value");

        async.process(event);
        asyncListener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);

        MuleEvent asyncEvent = asyncListener.event;

        // Event is copied, but session isn't
        assertNotSame(asyncEvent, event);
        assertNotSame(asyncEvent, event);
        assertNotSame(asyncEvent.getSession(), event.getSession());

        // Session properties available before async are available after too
        assertEquals(1, asyncEvent.getSession().getPropertyNamesAsSet().size());
        assertEquals("value", asyncEvent.getSession().getProperty("key"));

        // Session properties set after async are available in message processor
        // before async
        asyncEvent.getSession().setProperty("newKey", "newValue");
        assertEquals(2, asyncEvent.getSession().getPropertyNamesAsSet().size());
        assertEquals("newValue", asyncEvent.getSession().getProperty("newKey"));
        assertEquals(1, event.getSession().getPropertyNamesAsSet().size());
        assertNull(event.getSession().getProperty("newKey"));

        async.stop();
    }

    /**
     * MuleSession is not copied when async intercepting processor is used
     */
    @Test
    public void serializationSessionPropertyPropagation() throws Exception
    {
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, getTestService());

        event.getSession().setProperty("key", "value");

        ObjectSerializer serializer = muleContext.getObjectSerializer();
        MuleEvent deserializedEvent = serializer.deserialize(serializer.serialize(event));

        // Event and session are both copied
        assertNotSame(deserializedEvent, event);
        assertNotSame(deserializedEvent, event);
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
     * Serialization of a MuleSession with session properties fails (no warning is given)
     */
    @Test
    public void defaultSessionHandlerSessionPropertyPropagation() throws Exception
    {
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, getTestService());

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
     * Serialization of a MuleSession with session properties serializes only serializable properties
     */
    @Test
    public void serializationNonSerializableSessionPropertyPropagation() throws Exception
    {
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, getTestService());

        Object nonSerializable = new Object();
        event.getSession().setProperty("key", nonSerializable);
        message.setProperty("key2", "value2", PropertyScope.SESSION);

        ObjectSerializer serializer = muleContext.getObjectSerializer();
        MuleEvent deserialized = serializer.deserialize(serializer.serialize(event));

        // Serialization no longer fails as in 3.1.x/3.2.x

        assertEquals(nonSerializable, event.getSession().getProperty("key"));
        assertEquals("value2", event.getSession().getProperty("key2"));

        assertNotSame(deserialized, event);
        assertNull(deserialized.getSession().getProperty("key"));
        assertEquals("value2", deserialized.getSession().getProperty("key2"));
    }

    /**
     * Serialization of a MuleSession with session properties to message using SessionHandler serializes only
     * serializable properties
     */
    @Test
    public void defaultSessionHandlerNonSerializableSessionPropertyPropagation() throws Exception
    {
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, getTestService());

        Object nonSerializable = new Object();
        message.setProperty("key", nonSerializable, PropertyScope.SESSION);
        message.setProperty("key2", "value2", PropertyScope.SESSION);

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
        assertEquals(nonSerializable, event.getSession().getProperty("key"));
        assertEquals("value2", event.getSession().getProperty("key2"));

        // Non-serilizable session properties available before serialization are not
        // available after too
        assertEquals(1, newSession.getPropertyNamesAsSet().size());
        assertNull(newSession.getProperty("key"));
        assertEquals("value2", newSession.getProperty("key2"));
    }

    /**
     * When invoking a Flow directly session properties are preserved
     */
    @Test
    public void processFlowSessionPropertyPropagation() throws Exception
    {
        MuleEvent event = new DefaultMuleEvent(message, MessageExchangePattern.REQUEST_RESPONSE,
            getTestService());

        SensingNullMessageProcessor flowListener = new SensingNullMessageProcessor();
        Flow flow = new Flow("flow", muleContext);
        flow.setMessageProcessors(Collections.<MessageProcessor> singletonList(flowListener));
        flow.initialise();
        flow.start();

        Object nonSerializable = new Object();
        event.getSession().setProperty("key", "value");
        event.getSession().setProperty("key2", nonSerializable);

        flow.process(event);

        flowListener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);
        MuleEvent processedEvent = flowListener.event;

        // Event is copied, but session isn't
        assertNotSame(processedEvent, event);
        assertEquals(processedEvent, event);
        assertSame(processedEvent.getSession(), event.getSession());

        // Session properties available before new flow are available after too
        assertEquals(2, processedEvent.getSession().getPropertyNamesAsSet().size());
        assertEquals("value", processedEvent.getSession().getProperty("key"));
        assertEquals(nonSerializable, processedEvent.getSession().getProperty("key2"));

        // Session properties set after new flow are available in message processor
        // before new flow
        processedEvent.getSession().setProperty("newKey", "newValue");
        assertEquals(3, processedEvent.getSession().getPropertyNamesAsSet().size());
        assertEquals("newValue", processedEvent.getSession().getProperty("newKey"));
        assertEquals(3, event.getSession().getPropertyNamesAsSet().size());
        assertEquals("newValue", event.getSession().getProperty("newKey"));

        flow.stop();
        flow.dispose();
    }

}
