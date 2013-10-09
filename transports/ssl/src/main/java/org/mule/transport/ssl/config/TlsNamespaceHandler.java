/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ssl.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.specific.tls.ClientKeyStoreDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.KeyStoreDefinitionParser;
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
        registerBeanDefinitionParser("key-store", new KeyStoreDefinitionParser());
        registerBeanDefinitionParser("client", new ClientKeyStoreDefinitionParser());
        registerBeanDefinitionParser("server", new TrustStoreDefinitionParser());
        registerBeanDefinitionParser("protocol-handler", new ProtocolHandlerDefinitionParser());
    }

}
