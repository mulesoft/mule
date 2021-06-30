/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.hamcrest.core.IsNull;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.RoutingException;
import org.mule.config.i18n.MessageFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.hamcrest.Matchers.instanceOf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ExceptionsTestCase extends AbstractMuleTestCase
{

    @Test
    public void testExceptionChaining()
    {
        String rootMsg = "Root Test Exception Message";
        String msg = "Test Exception Message";

        Exception e = new MuleContextException(MessageFactory.createStaticMessage(msg), new DefaultMuleException(
                MessageFactory.createStaticMessage(rootMsg)));

        assertEquals(rootMsg, e.getCause().getMessage());
        assertEquals(msg, e.getMessage());
        assertEquals(e.getClass().getName() + ": " + msg, e.toString());
    }

    @Test
    public final void testRoutingExceptionNullMessageValidEndpoint() throws MuleException
    {
        OutboundEndpoint endpoint = mock(OutboundEndpoint.class);

        RoutingException rex = new RoutingException(null, endpoint);
        assertSame(endpoint, rex.getRoute());
    }

    @Test
    // MULE-19427
    public final void testRoutingSerializableWithNotSerializableEndpoint() throws IOException, ClassNotFoundException {
        OutboundEndpoint endpoint = mock(OutboundEndpoint.class);

        RoutingException rex = new RoutingException(null, endpoint);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(rex);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            Object o = ois.readObject();
            assertThat(o, instanceOf(RoutingException.class));
            RoutingException routingException = (RoutingException) o;
            assertThat(routingException.getFailingMessageProcessor(), is(IsNull.nullValue()));
        }
    }

    @Test
    // MULE-19427
    public final void testRoutingSerializableWithSerializableEndpoint() throws IOException, ClassNotFoundException {
        MessageProcessor endpoint = new TestMessageProcessor();

        RoutingException rex = new RoutingException(null, endpoint);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(rex);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            Object o = ois.readObject();
            assertThat(o, instanceOf(RoutingException.class));
            RoutingException routingException = (RoutingException) o;
            assertThat(routingException.getFailingMessageProcessor(), instanceOf(TestMessageProcessor.class));
            TestMessageProcessor testMessageProcessor = (TestMessageProcessor) routingException.getFailingMessageProcessor();
            assertThat(testMessageProcessor.field1, is("test"));
        }
    }

    private final static class TestMessageProcessor implements MessageProcessor, Serializable {

        String field1 = "test";

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException {
            return null;
        }
    }
}
