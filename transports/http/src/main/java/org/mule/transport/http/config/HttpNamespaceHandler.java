/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.config;



import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.generic.TextDefinitionParser;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.config.spring.parsers.specific.SecurityFilterDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.http.CacheControlHeader;
import org.mule.transport.http.CookieWrapper;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpPollingConnector;
import org.mule.transport.http.builder.HttpCookiesDefinitionParser;
import org.mule.transport.http.builder.HttpResponseDefinitionParser;
import org.mule.transport.http.components.HttpResponseBuilder;
import org.mule.transport.http.components.RestServiceWrapper;
import org.mule.transport.http.components.StaticResourceMessageProcessor;
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

        registerMuleBeanDefinitionParser("static-resource-handler",
                new MessageProcessorDefinitionParser(StaticResourceMessageProcessor.class));

        registerBeanDefinitionParser("response-builder", new MessageProcessorDefinitionParser(HttpResponseBuilder.class));
        registerMuleBeanDefinitionParser("header", new ChildMapEntryDefinitionParser("headers", "name", "value")).addCollection("headers");
        registerMuleBeanDefinitionParser("set-cookie", new HttpCookiesDefinitionParser("cookie", CookieWrapper.class)).registerPreProcessor(new CheckExclusiveAttributes(new String[][] {new String[] {"maxAge"}, new String[] {"expiryDate"}}));
        registerMuleBeanDefinitionParser("body", new TextDefinitionParser("body"));
        registerMuleBeanDefinitionParser("location", new HttpResponseDefinitionParser("header"));
        registerMuleBeanDefinitionParser("cache-control", new ChildDefinitionParser("cacheControl", CacheControlHeader.class));
        registerMuleBeanDefinitionParser("expires", new HttpResponseDefinitionParser("header"));




    }
}
