/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.jetty.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.ClientKeyStoreDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.KeyStoreDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.ProtocolHandlerDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.TrustStoreDefinitionParser;
import org.mule.transport.servlet.jetty.JettyHttpsConnector;
import org.mule.transport.servlet.jetty.WebappsConfiguration;

/**
 * Registers a Bean Definition Parser for handling <code><jetty:connector></code> elements.
 */
public class JettySslNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        registerMetaTransportEndpoints(JettyHttpsConnector.JETTY_SSL);
        registerConnectorDefinitionParser(JettyHttpsConnector.class);
        registerBeanDefinitionParser("webapps", new ChildDefinitionParser("webappsConfiguration", WebappsConfiguration.class, true));

        registerBeanDefinitionParser("tls-key-store", new KeyStoreDefinitionParser());
        registerBeanDefinitionParser("tls-client", new ClientKeyStoreDefinitionParser());
        registerBeanDefinitionParser("tls-server", new TrustStoreDefinitionParser());
        registerBeanDefinitionParser("tls-protocol-handler", new ProtocolHandlerDefinitionParser());
    }
}
