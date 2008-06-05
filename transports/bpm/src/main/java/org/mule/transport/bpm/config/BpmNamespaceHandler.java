/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.bpm.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.specific.RouterDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.routing.outbound.EndpointSelector;
import org.mule.transport.bpm.ProcessConnector;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Registers a Bean Definition Parsers for the "bpm" namespace.
 */
public class BpmNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public static final String PROCESS = "process";

    public void init()
    {
        registerStandardTransportEndpoints(ProcessConnector.PROTOCOL, new String[]{PROCESS}).addAlias(PROCESS, URIBuilder.PATH);
        registerConnectorDefinitionParser(ProcessConnector.class);
        registerBeanDefinitionParser("outbound-router", new BpmOutboundRouterDefinitionParser());
        // TODO MULE-3205
        //registerBeanDefinitionParser("component", new ComponentDefinitionParser(ProcessComponent.class));
    }

    /**
     * This is merely a shortcut for:
     *   <endpoint-selector-router evaluator="header" expression="MULE_BPM_ENDPOINT"> 
     */
    class BpmOutboundRouterDefinitionParser extends RouterDefinitionParser
    {
        public BpmOutboundRouterDefinitionParser()
        {
            super(EndpointSelector.class);
        }

        protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
        {
            builder.addPropertyValue("evaluator", "header");
            builder.addPropertyValue("expression", ProcessConnector.PROPERTY_ENDPOINT);
            super.parseChild(element, parserContext, builder);
        }
        
    }
}

