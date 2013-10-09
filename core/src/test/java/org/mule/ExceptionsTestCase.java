/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.RoutingException;
import org.mule.config.i18n.MessageFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

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
        OutboundEndpoint endpoint = Mockito.mock(OutboundEndpoint.class);

        RoutingException rex = new RoutingException(null, endpoint);
        assertSame(endpoint, rex.getRoute());
    }

}
