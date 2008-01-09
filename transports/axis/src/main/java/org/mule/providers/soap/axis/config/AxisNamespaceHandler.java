/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis.config;

import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.assembly.MapEntryCombiner;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.parsers.processors.AttributeConcatenation;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.properties.NestedMapWithAttributesDefinitionParser;
import org.mule.config.spring.parsers.specific.properties.SimplePropertyDefinitionParser;
import org.mule.config.spring.parsers.specific.properties.NestedListDefinitionParser;
import org.mule.config.spring.parsers.specific.properties.ElementInNestedMapDefinitionParser;
import org.mule.config.spring.parsers.specific.properties.ListPropertyDefinitionParser;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.providers.soap.axis.AxisConnector;

import java.util.Map;
import java.util.HashMap;

import org.w3c.dom.Element;

/**
 * Registers a Bean Definition Parser for handling <code>&lt;axis:connector&gt;</code> elements.
 */
public class AxisNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public static final String PROPERTIES = "properties";
    public static final Map USE_MAP = new HashMap();
    public static final Map STYLE_MAP = new HashMap();

    static
    {
        USE_MAP.put("RPC", "RPC");
        USE_MAP.put("DOCUMENT", "Document");
        USE_MAP.put("MESSAGE", "Message");
        USE_MAP.put("WRAPPED", "Wrapped");
        STYLE_MAP.put("ENCODED", "Encoded");
        STYLE_MAP.put("LITERAL", "Literal");
    }


    public void init()
    {
        // unusual propertires handling, so non-standard endpoint registration
        registerMuleBeanDefinitionParser("endpoint", new TransportGlobalEndpointDefinitionParser(AxisConnector.AXIS, TransportGlobalEndpointDefinitionParser.META, false, new String[]{}, new String[]{})).addMapping("use", USE_MAP).addMapping("style", STYLE_MAP);
        registerMuleBeanDefinitionParser("inbound-endpoint", new TransportEndpointDefinitionParser(AxisConnector.AXIS, TransportGlobalEndpointDefinitionParser.META, false, InboundEndpointFactoryBean.class, new String[]{}, new String[]{})).addMapping("use", USE_MAP).addMapping("style", STYLE_MAP);
        registerMuleBeanDefinitionParser("outbound-endpoint", new TransportEndpointDefinitionParser(AxisConnector.AXIS, TransportGlobalEndpointDefinitionParser.META, false, OutboundEndpointFactoryBean.class, new String[]{}, new String[]{})).addMapping("use", USE_MAP).addMapping("style", STYLE_MAP);
        registerBeanDefinitionParser("connector", new MuleOrphanDefinitionParser(AxisConnector.class, true));
        registerBeanDefinitionParser("supported-scheme", new ChildListEntryDefinitionParser("supportedSchemes", "value"));
        registerBeanDefinitionParser("soap-method", new ElementInNestedMapDefinitionParser(PROPERTIES, "soapMethods", "method"));
        registerBeanDefinitionParser("soap-parameter", new SoapParameterDefinitionParser());
        registerBeanDefinitionParser("soap-return", new SoapReturnDefinitionParser());
        registerMuleBeanDefinitionParser("soap-service", new NestedListDefinitionParser(PROPERTIES, "serviceInterfaces", "interface"));
        registerMuleBeanDefinitionParser("options", new NestedMapWithAttributesDefinitionParser(PROPERTIES, "axisOptions"));
        registerMuleBeanDefinitionParser("option", new SimplePropertyDefinitionParser());
        registerMuleBeanDefinitionParser("bean-type",
                new ParentContextDefinitionParser("connector", new ChildListEntryDefinitionParser("beanTypes", "interface"))
                        .otherwise(new NestedListDefinitionParser(PROPERTIES, "beanTypes", "interface")));
    }

    private static class SoapParameterDefinitionParser extends ListPropertyDefinitionParser
    {

        public static final String PARAMETER = "parameter";

        public SoapParameterDefinitionParser()
        {
            super(PARAMETER);
            registerPreProcessor(new AttributeConcatenation(PARAMETER, ";", new String[]{PARAMETER, "type", "mode"}));
        }

    }

    private static class SoapReturnDefinitionParser extends ListPropertyDefinitionParser
    {

        public SoapReturnDefinitionParser()
        {
            super(MapEntryCombiner.VALUE);
            registerPreProcessor(new PreProcessor()
            {
                public void preProcess(PropertyConfiguration config, Element element)
                {
                    element.setAttribute(MapEntryCombiner.VALUE, "return;" + element.getAttribute("type"));
                }
            });
        }

    }

}


