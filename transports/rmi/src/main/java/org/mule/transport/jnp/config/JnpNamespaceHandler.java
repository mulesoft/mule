/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
