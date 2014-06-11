/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty.functional;

import static junit.framework.Assert.assertEquals;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.servlet.jetty.JettyHttpConnector;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.nio.BlockingChannelConnector;
import org.junit.Rule;
import org.junit.Test;

public class JettyCustomConnectorTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");
    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Override
    protected String getConfigFile()
    {
        return "jetty-custom-connector.xml";
    }

    @Test
    public void defaultJettyConnector() throws Exception
    {
        JettyHttpConnector conn = (JettyHttpConnector) muleContext.getRegistry().lookupConnector(
            "defaultConnector");
        assertEquals(getDefaultJettyConnectorClass(), conn.getHttpServer().getConnectors()[0].getClass());
    }

    @Test
    public void customJettyConnector() throws Exception
    {
        JettyHttpConnector conn = (JettyHttpConnector) muleContext.getRegistry().lookupConnector(
            "customConnector");
        assertEquals(BlockingChannelConnector.class, conn.getHttpServer().getConnectors()[0].getClass());
    }

    protected Class<? extends Connector> getDefaultJettyConnectorClass()
    {
        return JettyHttpConnector.DEFAULT_JETTY_CONNECTOR_CLASS;
    }

}
