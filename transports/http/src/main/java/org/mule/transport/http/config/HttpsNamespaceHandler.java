/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.ClientKeyStoreDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.KeyStoreDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.ProtocolHandlerDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.TrustStoreDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpsConnector;
import org.mule.transport.http.HttpsPollingConnector;
import org.mule.transport.http.components.StaticResourceMessageProcessor;

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

        registerMuleBeanDefinitionParser("static-resource-handler",
                new MessageProcessorDefinitionParser(StaticResourceMessageProcessor.class));
    }

}
