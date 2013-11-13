/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.transport.tcp.issues.TcpSocketToAddressBindingTestCase;

import java.net.SocketException;

/**
 * Tests address binding by the Jetty transport. Extends {@link TcpSocketToAddressBindingTestCase} because, basically,
 * it's the same test but involving a different transport.
 */
public class JettyAddressBindingTestCase extends TcpSocketToAddressBindingTestCase
{
    public JettyAddressBindingTestCase() throws SocketException
    {
        super();
    }

    @Override
    protected String getConfigFile()
    {
        return "jetty-address-binding-test.xml";
    }

    @Override
    protected String getTransportName()
    {
        return "http";
    }
}
