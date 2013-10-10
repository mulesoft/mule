/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
