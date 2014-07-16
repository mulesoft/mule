/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.PreProcessor;
import org.mule.config.spring.parsers.assembly.MapEntryCombiner;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.processors.AttributeConcatenation;
import org.mule.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.properties.ElementInNestedMapDefinitionParser;
import org.mule.config.spring.parsers.specific.properties.ListPropertyDefinitionParser;
import org.mule.config.spring.parsers.specific.properties.NestedListDefinitionParser;
import org.mule.config.spring.parsers.specific.properties.NestedMapWithAttributesDefinitionParser;
import org.mule.config.spring.parsers.specific.properties.SimplePropertyDefinitionParser;
import org.mule.module.cxf.SoapConstants;
import org.mule.transport.soap.axis.AxisConnector;
import org.mule.transport.soap.axis.AxisMessageReceiver;
import org.mule.transport.soap.axis.component.WebServiceWrapperComponent;

import java.util.HashMap;
import java.util.Map;

import org.apache.axis.constants.Style;
import org.apache.axis.constants.Use;
import org.w3c.dom.Element;

/**
 * Registers a Bean Definition Parser for handling <code>&lt;axis:connector&gt;</code> elements.
 */
public class AxisNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public static final String PROPERTIES = "properties";
    public static final Map<String, String> USE_MAP = new HashMap<String, String>();
    public static final Map<String, String> STYLE_MAP = new HashMap<String, String>();

    static
    {
        USE_MAP.put("LITERAL", Use.LITERAL_STR);
        USE_MAP.put("ENCODED", Use.ENCODED_STR);

        STYLE_MAP.put("DOCUMENT", Style.DOCUMENT_STR);
        STYLE_MAP.put("MESSAGE", Style.MESSAGE_STR);
        STYLE_MAP.put("RPC", Style.RPC_STR);
        STYLE_MAP.put("WRAPPED", Style.WRAPPED_STR);
    }

    @Override
    public void init()
    {
        registerMetaTransportEndpoints(AxisConnector.AXIS).addMapping(AxisConnector.USE, USE_MAP).addMapping(AxisConnector.STYLE, STYLE_MAP);
        registerConnectorDefinitionParser(AxisConnector.class);
        registerBeanDefinitionParser("supported-scheme", new ChildListEntryDefinitionParser("supportedSchemes", "value"));
        registerBeanDefinitionParser("soap-method", new ElementInNestedMapDefinitionParser(PROPERTIES, AxisConnector.SOAP_METHODS, "method"));
        registerBeanDefinitionParser("soap-parameter", new SoapParameterDefinitionParser());
        registerBeanDefinitionParser("soap-return", new SoapReturnDefinitionParser());
        registerMuleBeanDefinitionParser("soap-service", new NestedListDefinitionParser(PROPERTIES, SoapConstants.SERVICE_INTERFACES, "interface"));
        registerMuleBeanDefinitionParser("options", new NestedMapWithAttributesDefinitionParser(PROPERTIES, AxisMessageReceiver.AXIS_OPTIONS));
        registerMuleBeanDefinitionParser("option", new SimplePropertyDefinitionParser());
        registerMuleBeanDefinitionParser("bean-type",
                new ParentContextDefinitionParser("connector", new ChildListEntryDefinitionParser(AxisMessageReceiver.BEAN_TYPES, "interface"))
                        .otherwise(new NestedListDefinitionParser(PROPERTIES, AxisMessageReceiver.BEAN_TYPES, "interface")));

        registerBeanDefinitionParser("wrapper-component", new ComponentDefinitionParser(WebServiceWrapperComponent.class));
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
                @Override
                public void preProcess(PropertyConfiguration config, Element element)
                {
                    element.setAttribute(MapEntryCombiner.VALUE, "return;" + element.getAttribute("type"));
                }
            });
        }
    }
}
