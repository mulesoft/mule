/*
 * $Id$
 * -----------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */

package org.mule.test;

import org.mule.MuleException;
import org.mule.config.i18n.Message;
import org.mule.impl.ImmutableMuleEndpoint;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.manager.ManagerException;
import org.mule.umo.routing.RoutingException;

import junit.framework.TestCase;

/**
 * @author Ross Mason
 */
public class ExceptionsTestCase extends TestCase
{

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testExceptionChaining()
    {
        String rootMsg = "Root Test Exception Message";
        String msg = "Test Exception Message";

        Exception e = new ManagerException(Message.createStaticMessage(msg),
                new MuleException(Message.createStaticMessage(rootMsg)));

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
        UMOImmutableEndpoint endpoint = new ImmutableMuleEndpoint("test://outbound", false);
        assertNotNull(endpoint);

        RoutingException rex = new RoutingException(null, endpoint);
        assertNotNull(rex);
        assertSame(endpoint, rex.getEndpoint());
    }

}
