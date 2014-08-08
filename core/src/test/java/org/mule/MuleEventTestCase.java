/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.Credentials;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.routing.MessageFilter;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.simple.ByteArrayToObject;
import org.mule.transformer.simple.SerializableToByteArray;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;


public class MuleEventTestCase extends AbstractMuleContextTestCase
{

//    @Test
//    public void testEventInitialise() throws Exception
//    {
//        String data = "Test Data";
//
//        DefaultMuleEvent event = (DefaultMuleEvent)getTestEvent(data, getTestService("orange", Orange.class));
//        RequestContext.setEvent(event);
//
//        assertEquals("MuleEvent data should equal " + data, data, event.getMessage().getPayload());
//        assertEquals("MuleEvent data should equal " + data, data, event.getMessageAsString());
//        assertEquals("MuleEvent data should equal " + data, data, event.transformMessage());
//        assertEquals("MuleEvent data should be a byte array 9 bytes in length", 9, event
//            .transformMessageToBytes().length);
//
//        assertEquals("MuleEvent data should be a byte array 9 bytes in length", 9,
//            event.getMessageAsBytes().length);
//        assertEquals("MuleEvent data should equal " + data, data, event.getSource());
//
//        assertEquals("MuleBeanPropertiesRule", event.getMessage().getProperty("MuleBeanPropertiesRule",
//            "MuleBeanPropertiesRule"));
//        event.getMessage().setProperty("Test", "Test1");
//
//        assertFalse(event.getMessage().getPropertyNames().isEmpty());
//        assertEquals("bla2", event.getMessage().getProperty("bla2", "bla2"));
//        assertEquals("Test1", event.getMessage().getProperty("Test"));
//        assertEquals("Test1", event.getMessage().getProperty("Test", "bla2"));
//        assertNotNull(event.getId());
//    }
//
//    @Test
//    public void testEventTransformer() throws Exception
//    {
//        String data = "Test Data";
//        ImmutableEndpoint endpoint = getTestOutboundEndpoint("Test",CollectionUtils.singletonList(new TestEventTransformer()));
//        MuleEvent event = getTestEvent(data, endpoint);
//        RequestContext.setEvent(event);
//
//        assertEquals("MuleEvent data should equal " + data, data, event.getMessage().getPayload());
//        assertEquals("MuleEvent data should equal " + data, data, event.getMessageAsString());
//        assertEquals("MuleEvent data should equal 'Transformed Test Data'", "Transformed Test Data", event
//            .transformMessage());
//        assertEquals("MuleEvent data should be a byte array 28 bytes in length", 21, event
//            .transformMessageToBytes().length);
//    }
//
//    @Test
//    public void testEventRewrite() throws Exception
//    {
//        String data = "Test Data";
//        ImmutableEndpoint endpoint = getTestOutboundEndpoint("Test", CollectionUtils.singletonList(new TestEventTransformer()));
//        DefaultMuleEvent event = new DefaultMuleEvent(new DefaultMuleMessage(data), endpoint,
//            getTestSession(getTestService("apple", Apple.class), muleContext), true,
//            new ResponseOutputStream(System.out));
//
//        assertNotNull(event.getId());
//        assertNotNull(event.getSession());
//        assertNotNull(event.getEndpoint());
//        assertNotNull(event.getOutputStream());
//        assertNotNull(event.getMessage());
//        assertEquals(data, event.getMessageAsString());
//
//        MuleEvent event2 = new DefaultMuleEvent(new DefaultMuleMessage("New Data"), event);
//        assertNotNull(event2.getId());
//        assertEquals(event.getId(), event2.getId());
//        assertNotNull(event2.getSession());
//        assertNotNull(event2.getEndpoint());
//        assertNotNull(event2.getOutputStream());
//        assertNotNull(event2.getMessage());
//        assertEquals("New Data", event2.getMessageAsString());
//
//    }
//
//    @Test
//    public void testProperties() throws Exception
//    {
//        MuleEvent prevEvent;
//        Properties props;
//        MuleMessage msg;
//        ImmutableEndpoint endpoint;
//        MuleEvent event;
//
//        // nowhere
//        prevEvent = getTestEvent("payload");
//        props = new Properties();
//        msg = new DefaultMuleMessage("payload", props);
//        props = new Properties();
//        endpoint = getTestOutboundEndpoint("Test", null, null, null, props);
//        event = new DefaultMuleEvent(msg, endpoint, prevEvent.getService(), prevEvent);
//        assertNull(event.getMessage().getProperty("prop"));
//
//        // in previous event => previous event
//        prevEvent.getMessage().setProperty("prop", "value0");
//        event = new DefaultMuleEvent(msg, endpoint, prevEvent.getService(), prevEvent);
//        assertEquals("value0", event.getMessage().getProperty("prop"));
//
//        // TODO check if this fragment can be removed
//        // in previous event + endpoint => endpoint
//        // This doesn't apply now as the previous event properties will be the same
//        // as the current event props
//        // props = new Properties();
//        // props.put("prop", "value2");
//        // endpoint.setProperties(props);
//        // event = new DefaultMuleEvent(msg, endpoint, prevEvent.getComponent(), prevEvent);
//        // assertEquals("value2", event.getProperty("prop"));
//
//        // in previous event + message => message
//        props = new Properties();
//        props.put("prop", "value1");
//        msg = new DefaultMuleMessage("payload", props);
//        endpoint = getTestOutboundEndpoint("Test");
//        event = new DefaultMuleEvent(msg, endpoint, prevEvent.getService(), prevEvent);
//        assertEquals("value1", event.getMessage().getProperty("prop"));
//
//        // in previous event + endpoint + message => message
//        props = new Properties();
//        props.put("prop", "value1");
//        msg = new DefaultMuleMessage("payload", props);
//
//        Properties props2 = new Properties();
//        props2.put("prop", "value2");
//        endpoint = getTestOutboundEndpoint("Test", null, null, null, props2);
//        event = new DefaultMuleEvent(msg, endpoint, prevEvent.getService(), prevEvent);
//        assertEquals("value1", event.getMessage().getProperty("prop"));
//
//    }
//
    /*
     * See http://mule.mulesoft.org/jira/browse/MULE-384 for details.
     */
    @Test
    public void testNoPasswordNoNullPointerException() throws Exception
    {
        // provide the username, but not the password, as is the case for SMTP
        // cannot set SMTP endpoint type, because the module doesn't have this
        // dependency
        ImmutableEndpoint endpoint = getTestOutboundEndpoint("AuthTest", "test://john.doe@xyz.fr");
        MuleEvent event = getTestEvent(new Object());
        Credentials credentials = event.getCredentials();
        assertNull("Credentials must not be created for endpoints without a password.", credentials);
    }

    @Test
    public void testEventSerialization() throws Exception
    {
        InboundEndpoint endpoint = getTestInboundEndpoint("Test", null, null,
                                                          new PayloadTypeFilter(Object.class), null, null);

        MuleEvent event = RequestContext.setEvent(getTestEvent("payload", endpoint));

        Transformer transformer = createSerializableToByteArrayTransformer();
        transformer.setMuleContext(muleContext);
        Serializable serialized = (Serializable) createSerializableToByteArrayTransformer().transform(event);
        assertNotNull(serialized);
        ByteArrayToObject trans = new ByteArrayToObject();
        trans.setMuleContext(muleContext);
        MuleEvent deserialized = (MuleEvent) trans.transform(serialized);

        // Assert that deserialized event is not null and has muleContext
        assertNotNull(deserialized);
        assertNotNull(deserialized.getMuleContext());

        // Assert that deserialized event has session with same id
        assertNotNull(deserialized.getSession());
        assertEquals(event.getSession().getId(), deserialized.getSession().getId());

        // Assert that deserialized event has service and that the service is the same instance
        assertNotNull(deserialized.getFlowConstruct());
        assertEquals(event.getFlowConstruct(), deserialized.getFlowConstruct());
        assertSame(event.getFlowConstruct(), deserialized.getFlowConstruct());

    }

    private Transformer createSerializableToByteArrayTransformer()
    {
        Transformer transformer = new SerializableToByteArray();
        transformer.setMuleContext(muleContext);

        return transformer;
    }

    @Test
    public void testEventSerializationRestart() throws Exception
    {
        // Create and register artifacts
        MuleEvent event = createEventToSerialize();
        muleContext.start();

        //Serialize
        Serializable serialized = (Serializable) createSerializableToByteArrayTransformer().transform(event);
        assertNotNull(serialized);

        // Simulate mule cold restart
        muleContext.dispose();
        muleContext = createMuleContext();
        muleContext.start();
        ByteArrayToObject trans = new ByteArrayToObject();
        trans.setMuleContext(muleContext);

        // Recreate and register artifacts (this would happen if using any kind of static config e.g. XML)
        createAndRegisterTransformersEndpointBuilderService();

        //Deserialize
        MuleEvent deserialized = (MuleEvent) trans.transform(serialized);

        // Assert that deserialized event is not null and has muleContext
        assertNotNull(deserialized);
        assertNotNull(deserialized.getMuleContext());

        // Assert that deserialized event has session with same id
        assertNotNull(deserialized.getSession());
        assertEquals(event.getSession().getId(), deserialized.getSession().getId());

        // Assert that deserialized event has service and that the service is the
        // same instance
        assertNotNull(deserialized.getFlowConstruct());

        Service service = (Service) event.getFlowConstruct();
        Service deserializedService = (Service) deserialized.getFlowConstruct();

        // Unable to test services for equality because of need for equals() everywhere.  See MULE-3720
        // assertEquals(event.getSession().getService(), deserialized.getSession().getService());
        assertEquals(service.getName(), deserializedService.getName());
        assertEquals(service.getInitialState(), deserializedService.getInitialState());
        assertEquals(service.getExceptionListener().getClass(), deserializedService.getExceptionListener().getClass());
        assertEquals(service.getComponent().getClass(), deserializedService.getComponent().getClass());

    }

    @Test
    public void testMuleCredentialsSerialization() throws Exception
    {
        String username = "mule";
        String password = "rulez";
        String url = "test://" + username + ":" + password + "@localhost";
        InboundEndpoint endpoint = getTestInboundEndpoint("Test", url);
        ByteArrayToObject trans = new ByteArrayToObject();
        trans.setMuleContext(muleContext);

        MuleEvent event = RequestContext.setEvent(getTestEvent("payload", endpoint));
        Serializable serialized = (Serializable) createSerializableToByteArrayTransformer().transform(event);
        assertNotNull(serialized);

        MuleEvent deserialized = (MuleEvent) trans.transform(serialized);
        assertNotNull(deserialized);

        Credentials credentials = deserialized.getCredentials();
        assertNotNull(credentials);
        assertEquals(username, credentials.getUsername());
        assertTrue(Arrays.equals(password.toCharArray(), credentials.getPassword()));
    }

    private MuleEvent createEventToSerialize() throws Exception
    {
        createAndRegisterTransformersEndpointBuilderService();
        InboundEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(
            muleContext.getRegistry().lookupEndpointBuilder("epBuilderTest"));
        Service service = muleContext.getRegistry().lookupService("appleService");
        return RequestContext.setEvent(getTestEvent("payload", service, endpoint));
    }


    @Test
    public void testMuleEventSerializationWithRawPayload() throws Exception
    {
        StringBuilder payload = new StringBuilder();
        //to reproduce issue we must try to serialize something with a payload bigger than 1020 bytes
        for (int i = 0; i < 108; i++)
        {
            payload.append("1234567890");
        }
        MuleEvent testEvent = getTestEvent(new ByteArrayInputStream(payload.toString().getBytes()));
        byte[] serializedEvent = muleContext.getObjectSerializer().serialize(testEvent);
        testEvent = muleContext.getObjectSerializer().deserialize(serializedEvent);

        assertArrayEquals((byte[])testEvent.getMessage().getPayload(), payload.toString().getBytes());
    }

    private void createAndRegisterTransformersEndpointBuilderService() throws Exception
    {
        Transformer trans1 = new TestEventTransformer();
        trans1.setName("OptimusPrime");
        muleContext.getRegistry().registerTransformer(trans1);

        Transformer trans2 = new TestEventTransformer();
        trans2.setName("Bumblebee");
        muleContext.getRegistry().registerTransformer(trans2);

        List<Transformer> transformers = new ArrayList<Transformer>();
        transformers.add(trans1);
        transformers.add(trans2);

        Filter filter = new PayloadTypeFilter(Object.class);
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("test://serializationTest",
            muleContext);
        endpointBuilder.setTransformers(transformers);
        endpointBuilder.setName("epBuilderTest");
        endpointBuilder.addMessageProcessor(new MessageFilter(filter));
        muleContext.getRegistry().registerEndpointBuilder("epBuilderTest", endpointBuilder);

        getTestService();
    }

    
    @Test(expected=UnsupportedOperationException.class)
    public void testFlowVarNamesAddImmutable() throws Exception
    {
        MuleEvent event = getTestEvent("whatever");
        event.setFlowVariable("test", "val");
        event.getFlowVariableNames().add("other");
    }
    
    public void testFlowVarNamesRemoveMutable() throws Exception
    {
        MuleEvent event = getTestEvent("whatever");
        event.setFlowVariable("test", "val");
        event.getFlowVariableNames().remove("test");
        assertNull(event.getFlowVariable("test"));
    }

    @Test
    public void testFlowVarsNotShared() throws Exception
    {
        MuleEvent event = getTestEvent("whatever");
        MuleMessage message = event.getMessage();
        message.setInvocationProperty("foo", "bar");

        MuleEvent copy = new DefaultMuleEvent(
            (MuleMessage) ((ThreadSafeAccess) event.getMessage()).newThreadCopy(), event, false, false);

        MuleMessage messageCopy = copy.getMessage();
        messageCopy.setInvocationProperty("foo", "bar2");

        assertEquals("bar", event.getFlowVariable("foo"));
        assertEquals("bar", message.getInvocationProperty("foo"));

        assertEquals("bar2", copy.getFlowVariable("foo"));
        assertEquals("bar2", messageCopy.getInvocationProperty("foo"));
    }

    @Test
    public void testFlowVarsShared() throws Exception
    {
        MuleEvent event = getTestEvent("whatever");
        MuleMessage message = event.getMessage();
        message.setInvocationProperty("foo", "bar");

        MuleEvent copy = new DefaultMuleEvent(
                (MuleMessage) ((ThreadSafeAccess) event.getMessage()).newThreadCopy(), event, false);

        MuleMessage messageCopy = copy.getMessage();
        messageCopy.setInvocationProperty("foo", "bar2");

        assertEquals("bar2", event.getFlowVariable("foo"));
        assertEquals("bar2", message.getInvocationProperty("foo"));

        assertEquals("bar2", copy.getFlowVariable("foo"));
        assertEquals("bar2", messageCopy.getInvocationProperty("foo"));
    }

    private static class TestEventTransformer extends AbstractTransformer
    {
        @Override
        public Object doTransform(Object src, String encoding) throws TransformerException
        {
            return "Transformed Test Data";
        }
    }

}
