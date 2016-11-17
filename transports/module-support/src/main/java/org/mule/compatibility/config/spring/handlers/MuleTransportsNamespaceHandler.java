/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.config.spring.handlers;

import static org.mule.runtime.config.spring.handlers.MuleNamespaceHandler.IDENTIFIER_PROPERTY;
import static org.mule.runtime.config.spring.handlers.MuleNamespaceHandler.VARIABLE_NAME_ATTRIBUTE;

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
import org.mule.compatibility.core.processor.simple.AddAttachmentProcessor;
import org.mule.compatibility.core.processor.simple.AddSessionVariableProcessor;
import org.mule.compatibility.core.processor.simple.CopyAttachmentsProcessor;
import org.mule.compatibility.core.processor.simple.RemoveAttachmentProcessor;
import org.mule.compatibility.core.processor.simple.RemoveSessionVariableProcessor;
import org.mule.compatibility.core.processor.simple.SetCorrelationIdTransformer;
import org.mule.compatibility.core.routing.EndpointDlqUntilSuccessful;
import org.mule.compatibility.core.routing.outbound.ExpressionRecipientList;
import org.mule.compatibility.core.routing.requestreply.SimpleAsyncEndpointRequestReplyRequester;
import org.mule.compatibility.core.transformer.simple.MessageProcessorTransformerAdaptor;
import org.mule.compatibility.module.cxf.builder.WebServiceMessageProcessorWithInboundEndpointBuilder;
import org.mule.compatibility.module.cxf.component.WebServiceWrapperComponent;
import org.mule.compatibility.module.cxf.config.JaxWsClientWithDecoupledEndpointFactoryBean;
import org.mule.compatibility.module.cxf.config.ProxyClientWithDecoupledEndpointFactoryBean;
import org.mule.compatibility.module.cxf.config.SimpleClientWithDecoupledEndpointFactoryBean;
import org.mule.compatibility.module.management.agent.DefaultTransportJmxSupportAgent;
import org.mule.extension.http.api.listener.HttpBasicAuthenticationFilter;
import org.mule.runtime.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.runtime.config.spring.parsers.MuleDefinitionParserConfiguration;
import org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.runtime.config.spring.parsers.processors.AddAttribute;
import org.mule.runtime.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ComponentDelegatingDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.DefaultNameMuleOrphanDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ExceptionStrategyDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.MessageProcessorWithDataTypeDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ResponseDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.SecurityFilterDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ThreadingProfileDefinitionParser;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.module.cxf.CxfConstants;

public class MuleTransportsNamespaceHandler extends AbstractMuleNamespaceHandler {

  @Override
  public void init() {
    // Core
    registerBeanDefinitionParser("until-successful",
                                 new ChildDefinitionParser("messageProcessor", EndpointDlqUntilSuccessful.class));
    registerBeanDefinitionParser("request-reply",
                                 new ChildDefinitionParser("messageProcessor", SimpleAsyncEndpointRequestReplyRequester.class));
    registerBeanDefinitionParser("default-exception-strategy",
                                 new ExceptionStrategyDefinitionParser(DefaultMessagingExceptionStrategy.class));

    // TODO MULE-10457 Remove this element and perform the wrapping transparently
    registerBeanDefinitionParser("mutator-transformer",
                                 new MessageProcessorDefinitionParser(MessageProcessorTransformerAdaptor.class));

    registerMuleBeanDefinitionParser("set-session-variable",
                                     new MessageProcessorWithDataTypeDefinitionParser(AddSessionVariableProcessor.class))
                                         .addAlias(VARIABLE_NAME_ATTRIBUTE, IDENTIFIER_PROPERTY);
    registerMuleBeanDefinitionParser("remove-session-variable",
                                     new MessageProcessorDefinitionParser(RemoveSessionVariableProcessor.class))
                                         .addAlias(VARIABLE_NAME_ATTRIBUTE, IDENTIFIER_PROPERTY);
    registerBeanDefinitionParser("set-attachment", new MessageProcessorDefinitionParser(AddAttachmentProcessor.class));
    registerBeanDefinitionParser("remove-attachment", new MessageProcessorDefinitionParser(RemoveAttachmentProcessor.class));
    registerBeanDefinitionParser("copy-attachments", new MessageProcessorDefinitionParser(CopyAttachmentsProcessor.class));
    // TODO MULE-10192
    registerBeanDefinitionParser("set-correlation-id", new MessageProcessorDefinitionParser(SetCorrelationIdTransformer.class));

    // Endpoint elements
    registerBeanDefinitionParser("endpoint", new OrphanEndpointDefinitionParser(EndpointURIEndpointBuilder.class));
    registerBeanDefinitionParser("inbound-endpoint", new ChildEndpointDefinitionParser(InboundEndpointFactoryBean.class));
    registerBeanDefinitionParser("outbound-endpoint", new ChildEndpointDefinitionParser(OutboundEndpointFactoryBean.class));
    registerBeanDefinitionParser("response", new ResponseDefinitionParser());

    // Connector elements
    registerBeanDefinitionParser("dispatcher-threading-profile",
                                 new ThreadingProfileDefinitionParser("dispatcherThreadingProfile",
                                                                      MuleProperties.OBJECT_DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE));
    registerBeanDefinitionParser("receiver-threading-profile",
                                 new ThreadingProfileDefinitionParser("receiverThreadingProfile",
                                                                      MuleProperties.OBJECT_DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE));
    registerBeanDefinitionParser("service-overrides", new ServiceOverridesDefinitionParser());
    registerBeanDefinitionParser("custom-connector", new MuleOrphanDefinitionParser(true));

    // Routing: Conditional Routers
    registerBeanDefinitionParser("recipient-list", new ChildDefinitionParser("messageProcessor", ExpressionRecipientList.class));

    // Scripting
    registerBeanDefinitionParser("component", new ComponentDelegatingDefinitionParser(DefaultJavaWithBindingComponent.class));
    registerBeanDefinitionParser("pooled-component",
                                 new ComponentDelegatingDefinitionParser(PooledJavaWithBindingsComponent.class));
    registerMuleBeanDefinitionParser("binding", new BindingDefinitionParser("interfaceBinding", DefaultInterfaceBinding.class));

    // Security
    SecurityFilterDefinitionParser securityFilterDefinitionParser =
        new SecurityFilterDefinitionParser(HttpBasicAuthenticationFilter.class);
    securityFilterDefinitionParser.addAlias("securityManager-ref", "securityManager");
    registerBeanDefinitionParser("http-security-filter", securityFilterDefinitionParser);

    // HTTP
    registerBeanDefinitionParser("config", new ChildDefinitionParser("extension", ConnectorConfiguration.class));

    // CXF
    MessageProcessorDefinitionParser jsParser =
        new MessageProcessorDefinitionParser(WebServiceMessageProcessorWithInboundEndpointBuilder.class);
    jsParser.registerPreProcessor(new AddAttribute("frontend", CxfConstants.JAX_WS_FRONTEND));
    registerBeanDefinitionParser("jaxws-service", jsParser);

    MessageProcessorDefinitionParser ssParser =
        new MessageProcessorDefinitionParser(WebServiceMessageProcessorWithInboundEndpointBuilder.class);
    ssParser.registerPreProcessor(new AddAttribute("frontend", CxfConstants.SIMPLE_FRONTEND));
    registerBeanDefinitionParser("simple-service", ssParser);

    registerBeanDefinitionParser("simple-client",
                                 new MessageProcessorDefinitionParser(SimpleClientWithDecoupledEndpointFactoryBean.class));
    registerBeanDefinitionParser("jaxws-client",
                                 new MessageProcessorDefinitionParser(JaxWsClientWithDecoupledEndpointFactoryBean.class));
    registerBeanDefinitionParser("proxy-client",
                                 new MessageProcessorDefinitionParser(ProxyClientWithDecoupledEndpointFactoryBean.class));

    registerBeanDefinitionParser("wrapper-component", new ComponentDefinitionParser(WebServiceWrapperComponent.class));

    // Management
    registerBeanDefinitionParser("publish-notifications",
                                 new DefaultNameMuleOrphanDefinitionParser(EndpointNotificationLoggerAgent.class));
    MuleDefinitionParserConfiguration defaultJmxParser =
        registerMuleBeanDefinitionParser("jmx-default-config",
                                         new DefaultNameMuleOrphanDefinitionParser(DefaultTransportJmxSupportAgent.class));
    defaultJmxParser.addAlias("registerMx4jAdapter", "loadMx4jAgent");
    defaultJmxParser.addAlias("registerLog4j", "loadLog4jAgent");
  }
}
