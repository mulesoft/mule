/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jnp.config;

import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.jnp.JnpConnector;
import org.mule.transport.rmi.config.RmiNamespaceHandler;

/**
 * Registers a Bean Definition Parser for handling <code>&lt;jnp:connector&gt;</code> elements.
 *
 */
public class JnpNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerMuleBeanDefinitionParser("endpoint", new TransportGlobalEndpointDefinitionParser(JnpConnector.JNP, TransportGlobalEndpointDefinitionParser.PROTOCOL, RmiNamespaceHandler.ADDRESS, RmiNamespaceHandler.PROPERTIES)).addAlias(RmiNamespaceHandler.OBJECT, URIBuilder.PATH);
        registerMuleBeanDefinitionParser("inbound-endpoint", new TransportEndpointDefinitionParser(JnpConnector.JNP, TransportGlobalEndpointDefinitionParser.PROTOCOL, InboundEndpointFactoryBean.class, RmiNamespaceHandler.ADDRESS, RmiNamespaceHandler.PROPERTIES)).addAlias(RmiNamespaceHandler.OBJECT, URIBuilder.PATH);
        registerMuleBeanDefinitionParser("outbound-endpoint", new TransportEndpointDefinitionParser(JnpConnector.JNP, TransportGlobalEndpointDefinitionParser.PROTOCOL, OutboundEndpointFactoryBean.class, RmiNamespaceHandler.ADDRESS, RmiNamespaceHandler.PROPERTIES)).addAlias(RmiNamespaceHandler.OBJECT, URIBuilder.PATH);
        registerConnectorDefinitionParser(JnpConnector.class);
    }

}
