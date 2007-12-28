/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.rmi.config;

import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.URIBuilder;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.config.spring.factories.ResponseEndpointFactoryBean;
import org.mule.config.MuleProperties;
import org.mule.providers.rmi.RmiConnector;

/**
 * Registers a Bean Definition Parser for handling <code>&lt;rmi:connector&gt;</code> elements.
 *
 */
public class RmiNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public static final String METHOD = MuleProperties.MULE_METHOD_PROPERTY;
    public static final String OBJECT = "object";
    public static final String[] PROPERTIES = new String[]{METHOD, RmiConnector.PROPERTY_SERVICE_METHOD_PARAM_TYPES};
    public static final String[] REQUIRED = new String[]{METHOD, OBJECT, URIBuilder.HOST, URIBuilder.PORT};

    public void init()
    {
        registerBeanDefinitionParser("endpoint", new TransportGlobalEndpointDefinitionParser(RmiConnector.RMI, TransportGlobalEndpointDefinitionParser.PROTOCOL, PROPERTIES, REQUIRED).addAlias(OBJECT, URIBuilder.PATH));
        registerBeanDefinitionParser("inbound-endpoint", new TransportEndpointDefinitionParser(RmiConnector.RMI, TransportGlobalEndpointDefinitionParser.PROTOCOL, InboundEndpointFactoryBean.class, PROPERTIES, REQUIRED).addAlias(OBJECT, URIBuilder.PATH));
        registerBeanDefinitionParser("outbound-endpoint", new TransportEndpointDefinitionParser(RmiConnector.RMI, TransportGlobalEndpointDefinitionParser.PROTOCOL, OutboundEndpointFactoryBean.class, PROPERTIES, REQUIRED).addAlias(OBJECT, URIBuilder.PATH));
        registerBeanDefinitionParser("response-endpoint", new TransportEndpointDefinitionParser(RmiConnector.RMI, TransportGlobalEndpointDefinitionParser.PROTOCOL, ResponseEndpointFactoryBean.class, PROPERTIES, REQUIRED).addAlias(OBJECT, URIBuilder.PATH));
        registerBeanDefinitionParser("connector", new MuleOrphanDefinitionParser(RmiConnector.class, true));
    }

}