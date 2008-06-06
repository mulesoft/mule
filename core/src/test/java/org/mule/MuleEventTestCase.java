/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.MuleEvent;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.simple.ByteArrayToObject;
import org.mule.transformer.simple.SerializableToByteArray;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MuleEventTestCase extends AbstractMuleTestCase
{

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
//    /**
//     * See http://mule.mulesource.org/jira/browse/MULE-384 for details.
//     */
//    public void testNoPasswordNoNullPointerException() throws Exception
//    {
//        // provide the username, but not the password, as is the case for SMTP
//        // cannot set SMTP endpoint type, because the module doesn't have this
//        // dependency
//        ImmutableEndpoint endpoint = getTestOutboundEndpoint("AuthTest", "test://john.doe@xyz.fr");
//        MuleEvent event = getTestEvent(new Object(), endpoint);
//        Credentials credentials = event.getCredentials();
//        assertNull("Credentials must not be created for endpoints without a password.", credentials);
//    }

    public void testEventSerialization() throws Exception
    {
        Transformer trans1 = new TestEventTransformer();
        trans1.setName("OptimusPrime");

        Transformer trans2 = new TestEventTransformer();
        trans2.setName("Bumblebee");

        List transformers = new ArrayList();
        transformers.add(trans1);
        transformers.add(trans2);

        
        ImmutableEndpoint endpoint = getTestOutboundEndpoint("Test", null, transformers, new PayloadTypeFilter(
            Object.class), null);

        MuleEvent event = RequestContext.setEvent(getTestEvent("payload", endpoint));
        Serializable serialized = (Serializable) new SerializableToByteArray().transform(event);
        assertNotNull(serialized);

        MuleEvent deserialized = (MuleEvent) new ByteArrayToObject().transform(serialized);
        
        // Assert that deserialized event is not null and has muleContext
        assertNotNull(deserialized);
        assertNotNull(deserialized.getMuleContext());

        // Assert that deserialized event has session with same id
        assertNotNull(deserialized.getSession());
        assertEquals(event.getSession().getId(), deserialized.getSession().getId());

        // Assert that deserialized event has service and that the service is the same instance
        assertNotNull(deserialized.getSession().getService());
        assertEquals(event.getSession().getService(), deserialized.getSession().getService());
        assertSame(event.getSession().getService(), deserialized.getSession().getService());

        // Assert that deserialized event has endpoint and that the endpoint is the same instance
        assertNotNull(deserialized.getEndpoint());    
        assertEquals(endpoint, deserialized.getEndpoint());

        List deserializedTransformers = deserialized.getEndpoint().getTransformers();
        assertEquals(2, deserializedTransformers.size());
        assertEquals(trans1.getName(), ((Transformer) deserializedTransformers.get(0)).getName());
        assertEquals(trans2.getName(), ((Transformer) deserializedTransformers.get(1)).getName());
        assertEquals(PayloadTypeFilter.class, deserialized.getEndpoint().getFilter().getClass());
    }


    private class TestEventTransformer extends AbstractTransformer
    {
        public Object doTransform(Object src, String encoding) throws TransformerException
        {
            return "Transformed Test Data";
        }
    }

}
