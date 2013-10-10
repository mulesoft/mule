/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.config;



import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.config.spring.parsers.specific.SecurityFilterDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpPollingConnector;
import org.mule.transport.http.components.RestServiceWrapper;
import org.mule.transport.http.filters.HttpBasicAuthenticationFilter;
import org.mule.transport.http.filters.HttpRequestWildcardFilter;
import org.mule.transport.http.transformers.HttpClientMethodResponseToObject;
import org.mule.transport.http.transformers.HttpRequestBodyToParamMap;
import org.mule.transport.http.transformers.HttpResponseToString;
import org.mule.transport.http.transformers.MuleMessageToHttpResponse;
import org.mule.transport.http.transformers.ObjectToHttpClientMethodRequest;

/**
 * Reigsters a Bean Definition Parser for handling <code><http:connector></code> elements.
 */
public class HttpNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        registerStandardTransportEndpoints(HttpConnector.HTTP, URIBuilder.SOCKET_ATTRIBUTES)
            .addAlias("contentType", HttpConstants.HEADER_CONTENT_TYPE)
            .addAlias("method", HttpConnector.HTTP_METHOD_PROPERTY);
        
        registerConnectorDefinitionParser(HttpConnector.class);
        registerBeanDefinitionParser("polling-connector", new MuleOrphanDefinitionParser(HttpPollingConnector.class, true));

        registerBeanDefinitionParser("rest-service-component", new ComponentDefinitionParser(RestServiceWrapper.class));
        registerBeanDefinitionParser("payloadParameterName", new ChildListEntryDefinitionParser("payloadParameterNames", ChildMapEntryDefinitionParser.VALUE));
        registerBeanDefinitionParser("requiredParameter", new ChildMapEntryDefinitionParser("requiredParams"));
        registerBeanDefinitionParser("optionalParameter", new ChildMapEntryDefinitionParser("optionalParams"));
        
        registerBeanDefinitionParser("http-response-to-object-transformer", new MessageProcessorDefinitionParser(HttpClientMethodResponseToObject.class));
        registerBeanDefinitionParser("http-response-to-string-transformer", new MessageProcessorDefinitionParser(HttpResponseToString.class));
        registerBeanDefinitionParser("object-to-http-request-transformer", new MessageProcessorDefinitionParser(ObjectToHttpClientMethodRequest.class));
        registerBeanDefinitionParser("message-to-http-response-transformer", new MessageProcessorDefinitionParser(MuleMessageToHttpResponse.class));
        registerBeanDefinitionParser("body-to-parameter-map-transformer", new MessageProcessorDefinitionParser(HttpRequestBodyToParamMap.class));

        registerBeanDefinitionParser("error-filter", new ParentDefinitionParser());
        registerBeanDefinitionParser("request-wildcard-filter", new FilterDefinitionParser(HttpRequestWildcardFilter.class));
        registerBeanDefinitionParser("basic-security-filter", new SecurityFilterDefinitionParser(HttpBasicAuthenticationFilter.class));
    }
}
