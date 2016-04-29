/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.MuleContextException;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;
import org.mockito.Mockito;

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
    public final void testRoutingExceptionNullMessageValidProcessor() throws MuleException
    {
        MessageProcessor processor = Mockito.mock(MessageProcessor.class);
        RoutingException rex = new RoutingException(null, processor);
        assertSame(processor, rex.getRoute());
    }

    @Test
    public final void testRoutingExceptionNullMessageValidEndpoint() throws MuleException
    {
        OutboundEndpoint endpoint = Mockito.mock(OutboundEndpoint.class);

        RoutingException rex = new RoutingException(null, endpoint);
        assertSame(endpoint, rex.getRoute());
    }

}
