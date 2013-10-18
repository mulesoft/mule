/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.udp.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.udp.UdpConnector;

/**
 * Reigsters a Bean Definition Parser for handling UDP specific elements.
 *
 */
public class UdpNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerStandardTransportEndpoints(UdpConnector.UDP, URIBuilder.SOCKET_ATTRIBUTES);
        registerConnectorDefinitionParser(UdpConnector.class);
    }

}
