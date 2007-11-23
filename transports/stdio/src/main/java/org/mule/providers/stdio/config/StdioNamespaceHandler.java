/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.stdio.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.URIBuilder;
import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.config.spring.factories.ResponseEndpointFactoryBean;
import org.mule.providers.stdio.PromptStdioConnector;
import org.mule.providers.stdio.StdioConnector;

/**
 * Reigsters a Bean Definition Parser for handling <code><tcp:connector></code> elements.
 *
 */
public class StdioNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public static final String SYSTEM_ATTRIBUTE = "system";
    public static final String SYSTEM_MAP =
            "IN=" + StdioConnector.STREAM_SYSTEM_IN +
            ",OUT=" + StdioConnector.STREAM_SYSTEM_OUT +
            ",ERR=" + StdioConnector.STREAM_SYSTEM_ERR;
    public static final String[] SYSTEM_ATTRIBUTE_ARRAY = new String[]{SYSTEM_ATTRIBUTE};

    public void init()
    {
        registerBeanDefinitionParser("endpoint", new TransportGlobalEndpointDefinitionParser(StdioConnector.STDIO, URIBuilder.PATH_ATTRIBUTES).addMapping(SYSTEM_ATTRIBUTE, SYSTEM_MAP).addAlias(SYSTEM_ATTRIBUTE, URIBuilder.PATH));
        registerBeanDefinitionParser("inbound-endpoint", new TransportEndpointDefinitionParser(StdioConnector.STDIO, InboundEndpointFactoryBean.class, SYSTEM_ATTRIBUTE_ARRAY).addMapping(SYSTEM_ATTRIBUTE, SYSTEM_MAP).addAlias(SYSTEM_ATTRIBUTE, URIBuilder.PATH));
        registerBeanDefinitionParser("outbound-endpoint", new TransportEndpointDefinitionParser(StdioConnector.STDIO, OutboundEndpointFactoryBean.class, SYSTEM_ATTRIBUTE_ARRAY).addMapping(SYSTEM_ATTRIBUTE, SYSTEM_MAP).addAlias(SYSTEM_ATTRIBUTE, URIBuilder.PATH));
        registerBeanDefinitionParser("response-endpoint", new TransportEndpointDefinitionParser(StdioConnector.STDIO, ResponseEndpointFactoryBean.class, SYSTEM_ATTRIBUTE_ARRAY).addMapping(SYSTEM_ATTRIBUTE, SYSTEM_MAP).addAlias(SYSTEM_ATTRIBUTE, URIBuilder.PATH));
        registerBeanDefinitionParser("connector", new MuleOrphanDefinitionParser(PromptStdioConnector.class, true));
    }

}
