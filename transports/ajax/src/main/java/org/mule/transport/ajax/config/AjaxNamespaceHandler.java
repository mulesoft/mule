/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.MuleDefinitionParserConfiguration;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.AddressedEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.ClientKeyStoreDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.KeyStoreDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.ProtocolHandlerDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.TrustStoreDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.ajax.container.AjaxServletConnector;
import org.mule.transport.ajax.embedded.AjaxConnector;

/**
 * Registers a Bean Definition Parser for handling <code>&lt;ajax:connector&gt;</code> elements and
 * <code>&lt;ajax:servlet-connector&gt;</code> elements.
 */
public class AjaxNamespaceHandler extends AbstractMuleNamespaceHandler
{
    @Override
    public void init()
    {
        //Embedded (default) endpoints
        MuleDefinitionParserConfiguration mdp = registerStandardTransportEndpoints(AjaxConnector.PROTOCOL, new String[]{"channel"});
        mdp.addAlias(AjaxConnector.CHANNEL_PROPERTY, URIBuilder.PATH);
        registerConnectorDefinitionParser(AjaxConnector.class);

        //SSL support (only for embedded)
        registerBeanDefinitionParser("key-store", new KeyStoreDefinitionParser());
        registerBeanDefinitionParser("client", new ClientKeyStoreDefinitionParser());
        registerBeanDefinitionParser("server", new TrustStoreDefinitionParser());
        registerBeanDefinitionParser("protocol-handler", new ProtocolHandlerDefinitionParser());

        //Servlet endpoints
        registerBeanDefinitionParser("servlet-connector", new MuleOrphanDefinitionParser(AjaxServletConnector.class, true));
        registerBeanDefinitionParser("servlet-endpoint", createServletGlobalEndpointParser(getGlobalEndpointBuilderBeanClass()));
        registerBeanDefinitionParser("servlet-inbound-endpoint", createServletEndpointParser(getInboundEndpointFactoryBeanClass()));
        registerBeanDefinitionParser("servlet-outbound-endpoint", createServletEndpointParser(getOutboundEndpointFactoryBeanClass()));
    }

    protected AddressedEndpointDefinitionParser createServletEndpointParser(Class<?> factoryBean)
    {
        AddressedEndpointDefinitionParser parser = new TransportEndpointDefinitionParser(AjaxServletConnector.PROTOCOL, false, factoryBean, new String[]{"channel"}, new String[]{});
        parser.addAlias(AjaxConnector.CHANNEL_PROPERTY, URIBuilder.PATH);
        return parser;
    }

    protected AddressedEndpointDefinitionParser createServletGlobalEndpointParser(Class<?> factoryBean)
    {
        AddressedEndpointDefinitionParser parser = new TransportGlobalEndpointDefinitionParser(AjaxServletConnector.PROTOCOL, false, factoryBean, new String[]{"channel"}, new String[]{});
        parser.addAlias(AjaxConnector.CHANNEL_PROPERTY, URIBuilder.PATH);
        return parser;
    }
}
