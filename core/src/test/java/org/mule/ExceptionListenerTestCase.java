/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule;

import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.exception.AbstractExceptionListener;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExceptionListenerTestCase extends AbstractMuleTestCase
{

    @Test
    public void testAddGoodEndpoint() throws Exception
    {
        AbstractExceptionListener router = new DefaultMessagingExceptionStrategy(null);
        OutboundEndpoint endpoint = Mockito.mock(OutboundEndpoint.class);
        router.addEndpoint(endpoint);
        assertNotNull(router.getMessageProcessors());
        assertTrue(router.getMessageProcessors().contains(endpoint));
    }

    @Test
    public void testSetGoodEndpoints() throws Exception
    {
        List<MessageProcessor> list = new ArrayList<MessageProcessor>();
        list.add(Mockito.mock(OutboundEndpoint.class));
        list.add(Mockito.mock(OutboundEndpoint.class));

        AbstractExceptionListener router = new DefaultMessagingExceptionStrategy(null);
        assertNotNull(router.getMessageProcessors());
        assertEquals(0, router.getMessageProcessors().size());

        router.addEndpoint(Mockito.mock(OutboundEndpoint.class));
        assertEquals(1, router.getMessageProcessors().size());

        router.setMessageProcessors(list);
        assertNotNull(router.getMessageProcessors());
        assertEquals(2, router.getMessageProcessors().size());
    }
}
