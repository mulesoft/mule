/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.servlet.ServletConnector;
import org.mule.transport.servlet.transformers.HttpRequestToByteArray;
import org.mule.transport.servlet.transformers.HttpRequestToInputStream;
import org.mule.transport.servlet.transformers.HttpRequestToParameterMap;

/**
 * Registers a Bean Definition Parser for handling <code><servlet:*></code> elements.
 */
public class ServletNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        registerStandardTransportEndpoints(ServletConnector.SERVLET, URIBuilder.PATH_ATTRIBUTES);
        registerConnectorDefinitionParser(ServletConnector.class);
        registerBeanDefinitionParser("http-request-to-parameter-map", new MessageProcessorDefinitionParser(HttpRequestToParameterMap.class));
        registerBeanDefinitionParser("http-request-to-input-stream", new MessageProcessorDefinitionParser(HttpRequestToInputStream.class));
        registerBeanDefinitionParser("http-request-to-byte-array", new MessageProcessorDefinitionParser(HttpRequestToByteArray.class));        
    }
}
