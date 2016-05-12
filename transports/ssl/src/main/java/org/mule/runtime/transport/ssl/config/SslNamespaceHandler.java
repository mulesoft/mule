/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.ssl.config;

import org.mule.runtime.config.spring.handlers.AbstractMuleTransportsNamespaceHandler;
import org.mule.runtime.config.spring.parsers.specific.tls.ClientKeyStoreDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.tls.KeyStoreDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.tls.ProtocolHandlerDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.tls.TrustStoreDefinitionParser;
import org.mule.runtime.core.endpoint.URIBuilder;
import org.mule.runtime.transport.ssl.SslConnector;

/**
 * Reigsters a Bean Definition Parser for handling <code><ssl:connector></code> elements.
 */
public class SslNamespaceHandler extends AbstractMuleTransportsNamespaceHandler
{

    @Override
    public void init()
    {
        registerStandardTransportEndpoints(SslConnector.SSL, URIBuilder.SOCKET_ATTRIBUTES);
        registerConnectorDefinitionParser(SslConnector.class);
        registerBeanDefinitionParser("key-store", new KeyStoreDefinitionParser());
        registerBeanDefinitionParser("client", new ClientKeyStoreDefinitionParser());
        registerBeanDefinitionParser("server", new TrustStoreDefinitionParser());
        registerBeanDefinitionParser("protocol-handler", new ProtocolHandlerDefinitionParser());
    }
    
}
