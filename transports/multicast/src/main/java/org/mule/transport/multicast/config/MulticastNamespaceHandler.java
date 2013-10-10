/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.multicast.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.multicast.MulticastConnector;

/**
 * Reigsters a Bean Definition Parser for handling <code><udp:connector></code> elements.
 *
 */
public class MulticastNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerStandardTransportEndpoints(MulticastConnector.MULTICAST, URIBuilder.SOCKET_ATTRIBUTES);
        registerConnectorDefinitionParser(MulticastConnector.class);
    }

}
