/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import static org.junit.Assert.assertEquals;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.MuleTestUtils.TestCallback;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.nio.BlockingChannelConnector;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.junit.Test;

public class JettyHttpConnectorTestCase extends AbstractMuleContextTestCase
{
    private static String CUSTOM_JETTY_CONNECTOR_CLASS = "org.eclipse.jetty.server.bio.SocketConnector";

    private JettyHttpConnector connector = new JettyHttpConnector(muleContext);

    @Test
    public void defaultConnectorImplementation() throws MuleException
    {
        assertEquals(SelectChannelConnector.class, connector.createJettyConnector().getClass());
    }

    @Test
    public void customConnectorImplementationViaSetter() throws MuleException
    {
        connector.setJettyConnectorClass(SocketConnector.class);
        assertEquals(SocketConnector.class, connector.createJettyConnector().getClass());
    }

    @Test
    public void customConnectorImplementationViaSystemProperty() throws Exception
    {
        testWithSystemProperty(JettyHttpConnector.JETTY_CONNECTOR_SYSTEM_PROPERTY,
            CUSTOM_JETTY_CONNECTOR_CLASS, new TestCallback()
            {
                public void run() throws InitialisationException
                {
                    connector = new JettyHttpConnector(muleContext);
                    assertEquals(SocketConnector.class, connector.createJettyConnector().getClass());
                }
            });
    }

    @Test
    public void customConnectorImplementationSetterTakesPrecedence() throws Exception
    {
        testWithSystemProperty(JettyHttpConnector.JETTY_CONNECTOR_SYSTEM_PROPERTY,
            CUSTOM_JETTY_CONNECTOR_CLASS, new TestCallback()
            {
                public void run() throws InitialisationException
                {
                    connector = new JettyHttpConnector(muleContext);
                    connector.setJettyConnectorClass(BlockingChannelConnector.class);
                    assertEquals(BlockingChannelConnector.class, connector.createJettyConnector().getClass());
                }
            });
    }

}
