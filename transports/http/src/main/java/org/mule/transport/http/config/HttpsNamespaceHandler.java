/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.ClientKeyStoreDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.KeyStoreDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.ProtocolHandlerDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.TrustStoreDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpsConnector;
import org.mule.transport.http.HttpsPollingConnector;

/**
 * Reigsters a Bean Definition Parser for handling <code><https:connector></code> elements.
 */
public class HttpsNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        registerStandardTransportEndpoints(HttpsConnector.HTTPS, URIBuilder.SOCKET_ATTRIBUTES)
            .addAlias("contentType", HttpConstants.HEADER_CONTENT_TYPE)
            .addAlias("method", HttpConnector.HTTP_METHOD_PROPERTY);
        
        registerConnectorDefinitionParser(HttpsConnector.class);
        registerBeanDefinitionParser("polling-connector", new MuleOrphanDefinitionParser(HttpsPollingConnector.class, true));

        registerBeanDefinitionParser("tls-key-store", new KeyStoreDefinitionParser());
        registerBeanDefinitionParser("tls-client", new ClientKeyStoreDefinitionParser());
        registerBeanDefinitionParser("tls-server", new TrustStoreDefinitionParser());
        registerBeanDefinitionParser("tls-protocol-handler", new ProtocolHandlerDefinitionParser());
    }

}
