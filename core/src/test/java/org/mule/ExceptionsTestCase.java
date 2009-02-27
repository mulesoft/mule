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

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.routing.RoutingException;
import org.mule.config.i18n.MessageFactory;
import org.mule.tck.AbstractMuleTestCase;

public class ExceptionsTestCase extends AbstractMuleTestCase
{

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

    public final void testRoutingExceptionNullMessageValidEndpoint() throws MuleException
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint("test://outbound");
        assertNotNull(endpoint);

        RoutingException rex = new RoutingException(null, endpoint);
        assertSame(endpoint, rex.getEndpoint());
    }

}
