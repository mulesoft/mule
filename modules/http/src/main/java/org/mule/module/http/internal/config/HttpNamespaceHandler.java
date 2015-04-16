/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.config;

import org.mule.api.config.MuleProperties;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.delegate.RootOrNestedElementBeanDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.config.spring.parsers.specific.ThreadingProfileDefinitionParser;
import org.mule.module.http.internal.HttpMapParam;
import org.mule.module.http.internal.HttpMessageBuilderRef;
import org.mule.module.http.internal.HttpParamType;
import org.mule.module.http.internal.HttpSingleParam;
import org.mule.module.http.internal.listener.DefaultHttpListener;
import org.mule.module.http.internal.listener.DefaultHttpListenerConfig;
import org.mule.module.http.internal.request.DefaultHttpRequester;
import org.mule.module.http.internal.request.FailureStatusCodeValidator;
import org.mule.module.http.internal.request.HttpAuthenticationType;
import org.mule.module.http.internal.request.NtlmProxyConfig;
import org.mule.module.http.internal.request.ProxyConfig;
import org.mule.module.http.internal.request.RamlApiConfiguration;
import org.mule.module.http.internal.request.SuccessStatusCodeValidator;

public class HttpNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        final ChildDefinitionParser listenerDefinitionParser = new ChildDefinitionParser("messageSource", DefaultHttpListener.class);
        registerBeanDefinitionParser("listener", listenerDefinitionParser);
        final MuleOrphanDefinitionParser listenerConfigDefinitionParser = new MuleOrphanDefinitionParser(DefaultHttpListenerConfig.class, true);
        registerBeanDefinitionParser("listener-config", listenerConfigDefinitionParser);

        registerBeanDefinitionParser("request", new MessageProcessorDefinitionParser(DefaultHttpRequester.class));
        registerBeanDefinitionParser("request-builder", new HttpRequestBuilderDefinitionParser());

        registerBeanDefinitionParser("builder", new ChildDefinitionParser("builder", HttpMessageBuilderRef.class));

        registerBeanDefinitionParser("query-param", new HttpMessageSingleParamDefinitionParser(HttpSingleParam.class, HttpParamType.QUERY_PARAM));
        registerBeanDefinitionParser("query-params", new HttpMessageSingleParamDefinitionParser(HttpMapParam.class, HttpParamType.QUERY_PARAM));

        registerBeanDefinitionParser("uri-param", new HttpMessageSingleParamDefinitionParser(HttpSingleParam.class, HttpParamType.URI_PARAM));
        registerBeanDefinitionParser("uri-params", new HttpMessageSingleParamDefinitionParser(HttpMapParam.class, HttpParamType.URI_PARAM));

        // No bean definition parser is registered for the "header" element because it already exists in the HTTP transport.
        // The HttpNamespaceHandler from the transport will register parsers both for the new and the old header element.
        registerBeanDefinitionParser("headers", new HttpMessageSingleParamDefinitionParser(HttpMapParam.class, HttpParamType.HEADER));

        registerBeanDefinitionParser("request-config", new HttpRequestConfigDefinitionParser());

        registerBeanDefinitionParser("proxy", new RootOrNestedElementBeanDefinitionParser(ProxyConfig.class, "proxyConfig"));
        registerBeanDefinitionParser("ntlm-proxy", new RootOrNestedElementBeanDefinitionParser(NtlmProxyConfig.class, "proxyConfig"));

        registerBeanDefinitionParser("basic-authentication", new HttpAuthenticationDefinitionParser(HttpAuthenticationType.BASIC));
        registerBeanDefinitionParser("digest-authentication", new HttpAuthenticationDefinitionParser(HttpAuthenticationType.DIGEST));
        registerBeanDefinitionParser("ntlm-authentication", new HttpAuthenticationDefinitionParser(HttpAuthenticationType.NTLM));

        registerBeanDefinitionParser("success-status-code-validator", new ChildDefinitionParser("responseValidator", SuccessStatusCodeValidator.class));
        registerBeanDefinitionParser("failure-status-code-validator", new ChildDefinitionParser("responseValidator", FailureStatusCodeValidator.class));

        registerBeanDefinitionParser("raml-api-configuration", new ChildDefinitionParser("apiConfiguration", RamlApiConfiguration.class));
        registerBeanDefinitionParser("worker-threading-profile", new ThreadingProfileDefinitionParser("workerThreadingProfile",  MuleProperties.OBJECT_DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE));
        registerBeanDefinitionParser("config", new ChildDefinitionParser("extension", HttpConfiguration.class));
    }
}
