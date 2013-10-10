/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ejb.config;

import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.ejb.EjbConnector;
import org.mule.transport.rmi.config.RmiNamespaceHandler;

public class EjbNamespaceHandler extends AbstractMuleNamespaceHandler 
{

    public void init()
    {
        registerMuleBeanDefinitionParser("endpoint", new TransportGlobalEndpointDefinitionParser(EjbConnector.EJB, TransportGlobalEndpointDefinitionParser.PROTOCOL, RmiNamespaceHandler.ADDRESS, RmiNamespaceHandler.PROPERTIES)).addAlias(RmiNamespaceHandler.OBJECT, URIBuilder.PATH);
        registerMuleBeanDefinitionParser("inbound-endpoint", new TransportEndpointDefinitionParser(EjbConnector.EJB, TransportGlobalEndpointDefinitionParser.PROTOCOL, InboundEndpointFactoryBean.class, RmiNamespaceHandler.ADDRESS, RmiNamespaceHandler.PROPERTIES)).addAlias(RmiNamespaceHandler.OBJECT, URIBuilder.PATH);
        registerMuleBeanDefinitionParser("outbound-endpoint", new TransportEndpointDefinitionParser(EjbConnector.EJB, TransportGlobalEndpointDefinitionParser.PROTOCOL, OutboundEndpointFactoryBean.class, RmiNamespaceHandler.ADDRESS, RmiNamespaceHandler.PROPERTIES)).addAlias(RmiNamespaceHandler.OBJECT, URIBuilder.PATH);
        registerConnectorDefinitionParser(EjbConnector.class);
    }

}
