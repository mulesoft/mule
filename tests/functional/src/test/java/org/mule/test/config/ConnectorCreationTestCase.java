/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.MuleServer;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.routing.UMOOutboundRouter;

import org.springframework.beans.factory.BeanCreationException;

public class ConnectorCreationTestCase extends FunctionalTestCase
{

    public ConnectorCreationTestCase()
    {
        setStartContext(false);
    }
    
    public void testAlwaysCreateUsingParamString() throws Exception
    {
        disposeManager();
        managementContext = getBuilder().configure("/org/mule/test/config/connector-creation-using-param.xml");
        MuleServer.setManagementContext(managementContext);
        managementContext.start();
        UMOComponent c = managementContext.getRegistry().lookupComponent("echo");
        UMOConnector con1 = c.getInboundRouter().getEndpoint("in").getConnector();
        UMOConnector con2 = ((UMOOutboundRouter)c.getOutboundRouter().getRouters().get(0)).getEndpoint("out").getConnector();
        assertTrue(!con1.equals(con2));
        managementContext.stop();
    }

    public void testCreateOnce() throws Exception
    {
        disposeManager();
        managementContext = getBuilder().configure("/org/mule/test/config/connector-create-once.xml");
        MuleServer.setManagementContext(managementContext);
        managementContext.start();
        UMOComponent c = managementContext.getRegistry().lookupComponent("echo");

        assertEquals(c.getInboundRouter().getEndpoint("in").getConnector(),
            ((UMOOutboundRouter)c.getOutboundRouter().getRouters().get(0)).getEndpoint("out").getConnector());
        managementContext.stop();
    }

    public void testCreateNeverUsingParamString() throws Exception
    {
        disposeManager();
        try
        {
            managementContext = getBuilder().configure("/org/mule/test/config/connector-create-never.xml");
            fail("Should fail as there is no existing test connector");
        }
        catch (BeanCreationException e)
        {
            // expected
        }
    }

}
