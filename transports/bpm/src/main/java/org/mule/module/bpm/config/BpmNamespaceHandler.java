/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bpm.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.RouterDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.module.bpm.Process;
import org.mule.module.bpm.ProcessComponent;
import org.mule.routing.outbound.EndpointSelector;
import org.mule.transport.bpm.ProcessConnector;
import org.mule.transport.bpm.jbpm.JBpmConnector;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Registers a Bean Definition Parsers for the "bpm" namespace.
 */
public class BpmNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public static final String PROCESS = "process";

    /** 
     * Allows simple configuration of jBPM from the generic "bpm" namespace.  Otherwise you would need to include both the 
     * "bpm" and "jbpm" namespaces in your config, which is not really justified.
     */
    public static final String JBPM_WRAPPER_CLASS = "org.mule.module.jbpm.Jbpm";

    public void init()
    {
        registerStandardTransportEndpoints(ProcessConnector.PROTOCOL, new String[]{PROCESS}).addAlias(PROCESS, URIBuilder.PATH);
        registerConnectorDefinitionParser(ProcessConnector.class);
        registerBeanDefinitionParser("outbound-router", new BpmOutboundRouterDefinitionParser());
        
        registerBeanDefinitionParser("process", new ProcessComponentDefinitionParser());

        registerMuleBeanDefinitionParser("process-definition", new ChildMapEntryDefinitionParser("processDefinitions", "name", "resource"));

        try
        {
            registerBeanDefinitionParser("jbpm", new MuleOrphanDefinitionParser(Class.forName(JBPM_WRAPPER_CLASS), true));
        }
        catch (ClassNotFoundException e)
        {
            logger.warn(e.getMessage());
        }
        registerBeanDefinitionParser("jbpm-connector", new MuleOrphanDefinitionParser(JBpmConnector.class, true));
    }

    /**
     * This is merely a shortcut for:
     *   <endpoint-selector-router evaluator="header" expression="MULE_BPM_ENDPOINT"> 
     * @deprecated It is recommended to configure BPM as a component rather than a transport for 3.x
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
            builder.addPropertyValue("expression", Process.PROPERTY_ENDPOINT);
            super.parseChild(element, parserContext, builder);
        }        
    }
    
    class ProcessComponentDefinitionParser extends ComponentDefinitionParser
    {
        public ProcessComponentDefinitionParser()
        {
            super(ProcessComponent.class);
            addAlias("processName", "name");
            addAlias("processDefinition", "resource");
        }
    }
}

