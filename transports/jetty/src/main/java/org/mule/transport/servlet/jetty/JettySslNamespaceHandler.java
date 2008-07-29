/*
 * $Id: JettyNamespaceHandler.java 12297 2008-07-11 17:54:29Z dandiep $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.specific.tls.ClientKeyStoreDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.KeyStoreDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.ProtocolHandlerDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.TrustStoreDefinitionParser;

/**
 * Registers a Bean Definition Parser for handling <code><jetty:connector></code> elements.
 */
public class JettySslNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {

        registerMetaTransportEndpoints(JettyHttpsConnector.JETTY_SSL);
        registerConnectorDefinitionParser(JettyHttpsConnector.class);

        registerBeanDefinitionParser("tls-key-store", new KeyStoreDefinitionParser());
        registerBeanDefinitionParser("tls-client", new ClientKeyStoreDefinitionParser());
        registerBeanDefinitionParser("tls-server", new TrustStoreDefinitionParser());
        registerBeanDefinitionParser("tls-protocol-handler", new ProtocolHandlerDefinitionParser());
    }

}
