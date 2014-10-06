/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.specific.tls.ClientKeyStoreDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.ProtocolHandlerDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.TrustStoreDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.ssl.TlsConnector;

/**
 * Reigsters a Bean Definition Parser for handling <code><tls:connector></code> elements.
 */
public class TlsNamespaceHandler extends AbstractMuleNamespaceHandler
{
    
    public void init()
    {
        registerStandardTransportEndpoints(TlsConnector.TLS, URIBuilder.SOCKET_ATTRIBUTES);
        registerConnectorDefinitionParser(TlsConnector.class);
        registerBeanDefinitionParser("key-store", new KeyStoreParentContextDefinitionParser());
        registerBeanDefinitionParser("client", new ClientKeyStoreDefinitionParser());
        registerBeanDefinitionParser("server", new TrustStoreDefinitionParser());
        registerBeanDefinitionParser("protocol-handler", new ProtocolHandlerDefinitionParser());
        registerBeanDefinitionParser("context", new TlsContextDefinitionParser());
        registerBeanDefinitionParser("trust-store", new TrustStoreTlsContextDefinitionParser());
    }

}
