/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.components.simple.EchoComponent;
import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.routing.UMOOutboundRouter;

public class ConnectorCreationTestCase extends AbstractMuleTestCase
{

    public void testAlwaysCreateUsingParamString() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder();
        builder.registerEndpoint("test://inbound?createConnector=ALWAYS", "in", true);
        builder.registerEndpoint("test://outbound?createConnector=ALWAYS", "out", false);
        builder.registerComponent(EchoComponent.class.getName(), "echo", "in", "out", null);
        UMOComponent c = builder.getModel().getComponent("echo");
        assertTrue(!c.getDescriptor().getInboundRouter().getEndpoint("in").getConnector().equals(
            ((UMOOutboundRouter)c.getDescriptor().getOutboundRouter().getRouters().get(0)).getEndpoint("out").getConnector()));
    }

    public void testCreateOnce() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder();
        builder.registerEndpoint("test://inbound", "in", true);
        builder.registerEndpoint("test://outbound", "out", false);
        builder.registerComponent(EchoComponent.class.getName(), "echo", "in", "out", null);
        UMOComponent c = builder.getModel().getComponent("echo");

        assertEquals(c.getDescriptor().getInboundRouter().getEndpoint("in").getConnector(),
            ((UMOOutboundRouter)c.getDescriptor().getOutboundRouter().getRouters().get(0)).getEndpoint("out").getConnector());
    }

    public void testCreateNeverUsingParamString() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder();
        try
        {
            builder.registerEndpoint("test://inbound?createConnector=NEVER", "in", true);
            fail("Should fail as there is no existing test connector");
        }
        catch (UMOException e)
        {
            // expected
        }
    }

}
