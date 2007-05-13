/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.config.i18n.MessageFactory;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.manager.ManagerException;
import org.mule.umo.routing.RoutingException;

public class ExceptionsTestCase extends AbstractMuleTestCase
{

    public void testExceptionChaining()
    {
        String rootMsg = "Root Test Exception Message";
        String msg = "Test Exception Message";

        Exception e = new ManagerException(MessageFactory.createStaticMessage(msg), new MuleException(
            MessageFactory.createStaticMessage(rootMsg)));

        assertEquals(rootMsg, e.getCause().getMessage());
        assertEquals(msg, e.getMessage());
        assertEquals(e.getClass().getName() + ": " + msg, e.toString());
    }

    public final void testRoutingExceptionNullUMOMessageNullUMOImmutableEndpoint() throws UMOException
    {
        RoutingException rex = new RoutingException(null, null);
        assertNotNull(rex);
    }

    public final void testRoutingExceptionNullUMOMessageValidUMOImmutableEndpoint() throws UMOException
    {
        UMOEndpoint endpoint = new MuleEndpoint("test://outbound", false);
        assertNotNull(endpoint);

        RoutingException rex = new RoutingException(null, endpoint);
        assertNotNull(rex);
        assertSame(endpoint, rex.getEndpoint());
    }

}
