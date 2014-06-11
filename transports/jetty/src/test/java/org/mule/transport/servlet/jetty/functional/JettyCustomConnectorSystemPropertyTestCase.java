/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty.functional;

import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.transport.servlet.jetty.JettyHttpConnector;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.junit.Rule;

public class JettyCustomConnectorSystemPropertyTestCase extends JettyCustomConnectorTestCase
{

    private static final Class<? extends Connector> DEFAULT_CONNECTOR_CLASS = SocketConnector.class;

    @Rule
    public SystemProperty systemProperty = new SystemProperty(
        JettyHttpConnector.JETTY_CONNECTOR_SYSTEM_PROPERTY, DEFAULT_CONNECTOR_CLASS.getCanonicalName());

    @Override
    protected Class<? extends Connector> getDefaultJettyConnectorClass()
    {
        return DEFAULT_CONNECTOR_CLASS;
    }

}
