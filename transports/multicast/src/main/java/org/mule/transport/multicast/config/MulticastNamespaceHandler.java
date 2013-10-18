/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
