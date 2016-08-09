/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.config;


import org.mule.compatibility.config.spring.handlers.AbstractMuleTransportsNamespaceHandler;
import org.mule.compatibility.core.endpoint.URIBuilder;
import org.mule.compatibility.transport.http.CacheControlHeader;
import org.mule.compatibility.transport.http.CookieWrapper;
import org.mule.compatibility.transport.http.HttpConnector;
import org.mule.compatibility.transport.http.HttpConstants;
import org.mule.compatibility.transport.http.HttpPollingConnector;
import org.mule.compatibility.transport.http.builder.HttpCookiesDefinitionParser;
import org.mule.compatibility.transport.http.builder.HttpHeaderDefinitionParser;
import org.mule.compatibility.transport.http.builder.HttpResponseDefinitionParser;
import org.mule.compatibility.transport.http.components.RestServiceWrapper;
import org.mule.compatibility.transport.http.components.StaticResourceMessageProcessor;
import org.mule.compatibility.transport.http.filters.HttpBasicAuthenticationFilter;
import org.mule.compatibility.transport.http.filters.HttpRequestWildcardFilter;
import org.mule.compatibility.transport.http.transformers.HttpClientMethodResponseToObject;
import org.mule.compatibility.transport.http.transformers.HttpRequestBodyToParamMap;
import org.mule.compatibility.transport.http.transformers.HttpResponseToString;
import org.mule.compatibility.transport.http.transformers.MuleMessageToHttpResponse;
import org.mule.compatibility.transport.http.transformers.ObjectToHttpClientMethodRequest;
import org.mule.runtime.config.spring.parsers.MuleDefinitionParser;
import org.mule.runtime.config.spring.parsers.MuleDefinitionParserConfiguration;
import org.mule.runtime.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.runtime.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.TextDefinitionParser;
import org.mule.runtime.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.runtime.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.SecurityFilterDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.TransformerMessageProcessorDefinitionParser;

import org.springframework.beans.factory.xml.BeanDefinitionParser;

/**
 * Reigsters a Bean Definition Parser for handling <code><http:connector></code> elements.
 *
 * This namespace handler now extends HttpNamespaceHandler from mule-module-http so that both projects can register bean
 * definition parsers for the same namespace (http).
 */
public class HttpNamespaceHandler extends AbstractMuleTransportsNamespaceHandler {

  public static final String HTTP_TRANSPORT_DEPRECATION_MESSAGE =
      "HTTP transport is deprecated and will be removed in Mule 4.0. Use HTTP module instead.";

  @Override
  public void init() {
    registerStandardTransportEndpoints(HttpConnector.HTTP, URIBuilder.SOCKET_ATTRIBUTES)
        .addAlias("contentType", HttpConstants.HEADER_CONTENT_TYPE).addAlias("method", HttpConnector.HTTP_METHOD_PROPERTY);

    registerDeprecatedConnectorDefinitionParser(HttpConnector.class);
    registerDeprecatedBeanDefinitionParser("polling-connector", new MuleOrphanDefinitionParser(HttpPollingConnector.class, true));

    registerDeprecatedBeanDefinitionParser("rest-service-component", new ComponentDefinitionParser(RestServiceWrapper.class));
    registerDeprecatedBeanDefinitionParser("payloadParameterName",
                                           new ChildListEntryDefinitionParser("payloadParameterNames",
                                                                              ChildMapEntryDefinitionParser.VALUE));
    registerDeprecatedBeanDefinitionParser("requiredParameter", new ChildMapEntryDefinitionParser("requiredParams"));
    registerDeprecatedBeanDefinitionParser("optionalParameter", new ChildMapEntryDefinitionParser("optionalParams"));

    registerDeprecatedBeanDefinitionParser("http-response-to-object-transformer",
                                           new TransformerMessageProcessorDefinitionParser(HttpClientMethodResponseToObject.class));
    registerDeprecatedBeanDefinitionParser("http-response-to-string-transformer",
                                           new TransformerMessageProcessorDefinitionParser(HttpResponseToString.class));
    registerDeprecatedBeanDefinitionParser("object-to-http-request-transformer",
                                           new TransformerMessageProcessorDefinitionParser(ObjectToHttpClientMethodRequest.class));
    registerDeprecatedBeanDefinitionParser("message-to-http-response-transformer",
                                           new TransformerMessageProcessorDefinitionParser(MuleMessageToHttpResponse.class));
    registerDeprecatedBeanDefinitionParser("body-to-parameter-map-transformer",
                                           new TransformerMessageProcessorDefinitionParser(HttpRequestBodyToParamMap.class));

    registerDeprecatedBeanDefinitionParser("error-filter", new ParentDefinitionParser());
    registerDeprecatedBeanDefinitionParser("request-wildcard-filter",
                                           new FilterDefinitionParser(HttpRequestWildcardFilter.class));
    registerDeprecatedBeanDefinitionParser("basic-security-filter",
                                           new SecurityFilterDefinitionParser(HttpBasicAuthenticationFilter.class));

    registerDeprecatedMuleBeanDefinitionParser("static-resource-handler",
                                               new MessageProcessorDefinitionParser(StaticResourceMessageProcessor.class));

    registerDeprecatedBeanDefinitionParser("response-builder", new HttpResponseBuilderDefinitionParser("responseBuilder"));
    registerDeprecatedBeanDefinitionParser("error-response-builder",
                                           new HttpResponseBuilderDefinitionParser("errorResponseBuilder"));

    registerDeprecatedMuleBeanDefinitionParser("header", new HttpHeaderDefinitionParser()).addCollection("headers");
    registerDeprecatedMuleBeanDefinitionParser("set-cookie", new HttpCookiesDefinitionParser("cookie", CookieWrapper.class))
        .registerPreProcessor(new CheckExclusiveAttributes(new String[][] {new String[] {"maxAge"},
            new String[] {"expiryDate"}}));
    registerDeprecatedMuleBeanDefinitionParser("body", new TextDefinitionParser("body"));
    registerDeprecatedMuleBeanDefinitionParser("location", new HttpResponseDefinitionParser("header"));
    registerDeprecatedMuleBeanDefinitionParser("cache-control",
                                               new ChildDefinitionParser("cacheControl", CacheControlHeader.class));
    registerDeprecatedMuleBeanDefinitionParser("expires", new HttpResponseDefinitionParser("header"));
  }

  protected void registerDeprecatedBeanDefinitionParser(String elementName, BeanDefinitionParser parser) {
    registerDeprecatedBeanDefinitionParser(elementName, parser, HTTP_TRANSPORT_DEPRECATION_MESSAGE);
  }

  protected MuleDefinitionParserConfiguration registerDeprecatedMuleBeanDefinitionParser(String elementName,
                                                                                         MuleDefinitionParser parser) {
    return registerDeprecatedMuleBeanDefinitionParser(elementName, parser, HTTP_TRANSPORT_DEPRECATION_MESSAGE);
  }

  protected MuleDefinitionParserConfiguration registerDeprecatedConnectorDefinitionParser(Class connectorClass) {
    return registerDeprecatedConnectorDefinitionParser(connectorClass, HTTP_TRANSPORT_DEPRECATION_MESSAGE);
  }
}
