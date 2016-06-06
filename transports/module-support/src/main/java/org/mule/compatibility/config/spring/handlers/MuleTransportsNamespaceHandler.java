/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.config.spring.handlers;

import org.mule.compatibility.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.compatibility.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.compatibility.config.spring.parsers.specific.BindingDefinitionParser;
import org.mule.compatibility.config.spring.parsers.specific.ServiceOverridesDefinitionParser;
import org.mule.compatibility.config.spring.parsers.specific.endpoint.support.ChildEndpointDefinitionParser;
import org.mule.compatibility.config.spring.parsers.specific.endpoint.support.OrphanEndpointDefinitionParser;
import org.mule.compatibility.core.agent.EndpointNotificationLoggerAgent;
import org.mule.compatibility.core.component.DefaultInterfaceBinding;
import org.mule.compatibility.core.component.DefaultJavaWithBindingComponent;
import org.mule.compatibility.core.component.PooledJavaWithBindingsComponent;
import org.mule.compatibility.core.config.ConnectorConfiguration;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.compatibility.core.routing.EndpointDlqUntilSuccessful;
import org.mule.compatibility.core.routing.outbound.ExpressionRecipientList;
import org.mule.compatibility.core.routing.requestreply.SimpleAsyncEndpointRequestReplyRequester;
import org.mule.compatibility.module.cxf.builder.WebServiceMessageProcessorWithInboundEndpointBuilder;
import org.mule.compatibility.module.cxf.component.WebServiceWrapperComponent;
import org.mule.compatibility.module.cxf.config.JaxWsClientWithDecoupledEndpointFactoryBean;
import org.mule.compatibility.module.cxf.config.ProxyClientWithDecoupledEndpointFactoryBean;
import org.mule.compatibility.module.cxf.config.SimpleClientWithDecoupledEndpointFactoryBean;
import org.mule.runtime.config.spring.factories.PollingMessageSourceFactoryBean;
import org.mule.runtime.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.runtime.config.spring.parsers.processors.AddAttribute;
import org.mule.runtime.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ComponentDelegatingDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.DefaultNameMuleOrphanDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ExceptionStrategyDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ResponseDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.SecurityFilterDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ThreadingProfileDefinitionParser;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.module.cxf.CxfConstants;
import org.mule.runtime.module.http.internal.filter.HttpBasicAuthenticationFilter;

public class MuleTransportsNamespaceHandler extends AbstractMuleNamespaceHandler
{

    @Override
    public void init()
    {
        // Core
        registerBeanDefinitionParser("until-successful", new ChildDefinitionParser("messageProcessor", EndpointDlqUntilSuccessful.class));
        registerBeanDefinitionParser("request-reply", new ChildDefinitionParser("messageProcessor", SimpleAsyncEndpointRequestReplyRequester.class));
        registerBeanDefinitionParser("default-exception-strategy", new ExceptionStrategyDefinitionParser(DefaultMessagingExceptionStrategy.class));

        // Endpoint elements
        registerBeanDefinitionParser("endpoint", new OrphanEndpointDefinitionParser(EndpointURIEndpointBuilder.class));
        registerBeanDefinitionParser("inbound-endpoint", new ChildEndpointDefinitionParser(InboundEndpointFactoryBean.class));
        registerBeanDefinitionParser("outbound-endpoint", new ChildEndpointDefinitionParser(OutboundEndpointFactoryBean.class));
        registerBeanDefinitionParser("poll", new ChildEndpointDefinitionParser(PollingMessageSourceFactoryBean.class));
        registerBeanDefinitionParser("response", new ResponseDefinitionParser());

        // Connector elements
        registerBeanDefinitionParser("dispatcher-threading-profile",
                new ThreadingProfileDefinitionParser("dispatcherThreadingProfile", MuleProperties.OBJECT_DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE));
        registerBeanDefinitionParser("receiver-threading-profile", new ThreadingProfileDefinitionParser("receiverThreadingProfile", MuleProperties.OBJECT_DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE));
        registerBeanDefinitionParser("service-overrides", new ServiceOverridesDefinitionParser());
        registerBeanDefinitionParser("custom-connector", new MuleOrphanDefinitionParser(true));

        // Routing: Conditional Routers
        registerBeanDefinitionParser("recipient-list", new ChildDefinitionParser("messageProcessor", ExpressionRecipientList.class));

        // Scripting
        registerBeanDefinitionParser("component", new ComponentDelegatingDefinitionParser(DefaultJavaWithBindingComponent.class));
        registerBeanDefinitionParser("pooled-component", new ComponentDelegatingDefinitionParser(PooledJavaWithBindingsComponent.class));
        registerMuleBeanDefinitionParser("binding", new BindingDefinitionParser("interfaceBinding", DefaultInterfaceBinding.class));

        // Security
        SecurityFilterDefinitionParser securityFilterDefinitionParser = new SecurityFilterDefinitionParser(HttpBasicAuthenticationFilter.class);
        securityFilterDefinitionParser.addAlias("securityManager-ref", "securityManager");
        registerBeanDefinitionParser("http-security-filter", securityFilterDefinitionParser);

        // HTTP
        registerBeanDefinitionParser("config", new ChildDefinitionParser("extension", ConnectorConfiguration.class));

        // CXF
        MessageProcessorDefinitionParser jsParser = new MessageProcessorDefinitionParser(WebServiceMessageProcessorWithInboundEndpointBuilder.class);
        jsParser.registerPreProcessor(new AddAttribute("frontend", CxfConstants.JAX_WS_FRONTEND));
        registerBeanDefinitionParser("jaxws-service", jsParser);

        MessageProcessorDefinitionParser ssParser = new MessageProcessorDefinitionParser(WebServiceMessageProcessorWithInboundEndpointBuilder.class);
        ssParser.registerPreProcessor(new AddAttribute("frontend", CxfConstants.SIMPLE_FRONTEND));
        registerBeanDefinitionParser("simple-service", ssParser);

        registerBeanDefinitionParser("simple-client", new MessageProcessorDefinitionParser(SimpleClientWithDecoupledEndpointFactoryBean.class));
        registerBeanDefinitionParser("jaxws-client", new MessageProcessorDefinitionParser(JaxWsClientWithDecoupledEndpointFactoryBean.class));
        registerBeanDefinitionParser("proxy-client", new MessageProcessorDefinitionParser(ProxyClientWithDecoupledEndpointFactoryBean.class));

        registerBeanDefinitionParser("wrapper-component", new ComponentDefinitionParser(WebServiceWrapperComponent.class));

        // Management
        registerBeanDefinitionParser("publish-notifications", new DefaultNameMuleOrphanDefinitionParser(EndpointNotificationLoggerAgent.class));
        registerBeanDefinitionParser("outbound-endpoint", new ChildEndpointDefinitionParser(OutboundEndpointFactoryBean.class));

    }
}
