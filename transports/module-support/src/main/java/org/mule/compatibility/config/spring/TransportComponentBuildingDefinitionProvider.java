/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.config.spring;

import static org.mule.compatibility.config.spring.TransportXmlNamespaceInfoProvider.TRANSPORTS_NAMESPACE_NAME;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleReferenceParameter;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromUndefinedSimpleAttributes;
import static org.mule.runtime.config.spring.dsl.api.TypeDefinition.fromType;
import static org.mule.runtime.config.spring.dsl.model.CoreComponentBuildingDefinitionProvider.getMuleMessageTransformerBaseBuilder;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;

import org.mule.compatibility.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.compatibility.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.compatibility.core.transformer.simple.AddAttachmentTransformer;
import org.mule.compatibility.core.transformer.simple.CopyAttachmentsTransformer;
import org.mule.compatibility.core.transformer.simple.RemoveAttachmentTransformer;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinitionProvider;
import org.mule.runtime.config.spring.dsl.api.TypeConverter;
import org.mule.runtime.config.spring.factories.MessageProcessorChainFactoryBean;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.retry.RetryPolicy;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.processor.AbstractRedeliveryPolicy;
import org.mule.runtime.core.transaction.MuleTransactionConfig;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Provider for {@code ComponentBuildingDefinition}s required to create transport runtime object.
 */
public class TransportComponentBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider {

  public static final String OUTBOUND_ENDPOINT_ELEMENT = "outbound-endpoint";
  public static final String ENDPOINT_ELEMENT = "endpoint";
  public static final String INBOUND_ENDPOINT_ELEMENT = "inbound-endpoint";
  private static final String ENDPOINT_RESPONSE_ELEMENT = "response";

  private ComponentBuildingDefinition.Builder baseDefinition =
      new ComponentBuildingDefinition.Builder().withNamespace(TRANSPORTS_NAMESPACE_NAME);

  @Override
  public void init(MuleContext muleContext) {}

  @Override
  public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {
    LinkedList<ComponentBuildingDefinition> componentBuildingDefinitions = new LinkedList<>();
    componentBuildingDefinitions.add(getInboundEndpointBuildingDefinitionBuilder().build());
    componentBuildingDefinitions.add(getOutboundEndpointBuildingDefinitionBuilder().build());
    componentBuildingDefinitions.add(getEndpointBuildingDefinitionBuilder().build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(ENDPOINT_RESPONSE_ELEMENT).withTypeDefinition(fromType(MessageProcessor.class))
            .withObjectFactoryType(MessageProcessorChainFactoryBean.class).build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("service-overrides")
        .withObjectFactoryType(ServiceOverridesObjectFactory.class).withTypeDefinition(fromType(Map.class))
        .withSetterParameterDefinition("messageReceiver", fromSimpleParameter("messageReceiver").build())
        .withSetterParameterDefinition("transactedMessageReceiver", fromSimpleParameter("transactedMessageReceiver").build())
        .withSetterParameterDefinition("xaTransactedMessageReceiver", fromSimpleParameter("xaTransactedMessageReceiver").build())
        .withSetterParameterDefinition("dispatcherFactory", fromSimpleParameter("dispatcherFactory").build())
        .withSetterParameterDefinition("inboundTransformer", fromSimpleParameter("inboundTransformer").build())
        .withSetterParameterDefinition("outboundTransformer", fromSimpleParameter("outboundTransformer").build())
        .withSetterParameterDefinition("responseTransformer", fromSimpleParameter("responseTransformer").build())
        .withSetterParameterDefinition("endpointBuilder", fromSimpleParameter("endpointBuilder").build())
        .withSetterParameterDefinition("messageFactory", fromSimpleParameter("messageFactory").build())
        .withSetterParameterDefinition("serviceFinder", fromSimpleParameter("serviceFinder").build())
        .withSetterParameterDefinition("sessionHandler", fromSimpleParameter("sessionHandler").build())
        .withSetterParameterDefinition("inboundExchangePatterns", fromSimpleParameter("inboundExchangePatterns").build())
        .withSetterParameterDefinition("outboundExchangePatterns", fromSimpleParameter("outboundExchangePatterns").build())
        .withSetterParameterDefinition("defaultExchangePattern", fromSimpleParameter("defaultExchangePattern").build())
        .build());
    componentBuildingDefinitions.add(getMuleMessageTransformerBaseBuilder()
        .withIdentifier("set-attachment")
        .withTypeDefinition(fromType(AddAttachmentTransformer.class))
        .withSetterParameterDefinition("attachmentName", fromSimpleParameter("attachmentName").build())
        .withSetterParameterDefinition("value", fromSimpleParameter("value").build())
        .withSetterParameterDefinition("contentType", fromSimpleParameter("contentType").build())
        .build());
    componentBuildingDefinitions.add(getMuleMessageTransformerBaseBuilder()
        .withIdentifier("remove-attachment")
        .withTypeDefinition(fromType(RemoveAttachmentTransformer.class))
        .withSetterParameterDefinition("attachmentName", fromSimpleParameter("attachmentName").build())
        .build());
    componentBuildingDefinitions.add(getMuleMessageTransformerBaseBuilder()
        .withIdentifier("copy-attachments")
        .withTypeDefinition(fromType(CopyAttachmentsTransformer.class))
        .withSetterParameterDefinition("attachmentName", fromSimpleParameter("attachmentName").build())
        .build());
    return componentBuildingDefinitions;
  }

  protected ComponentBuildingDefinition.Builder getBaseConnector() {
    return baseDefinition.copy().withIdentifier("connector")
        .withConstructorParameterDefinition(fromReferenceObject(MuleContext.class).build())
        .withSetterParameterDefinition("serviceOverrides", fromChildConfiguration(Map.class).build())
        .withSetterParameterDefinition("retryPolicyTemplate", fromChildConfiguration(RetryPolicyTemplate.class).build());
  }

  protected ComponentBuildingDefinition.Builder getOutboundEndpointBuildingDefinitionBuilder() {
    return baseDefinition.copy().withIdentifier(OUTBOUND_ENDPOINT_ELEMENT)
        .withObjectFactoryType(OutboundEndpointFactoryBean.class).withTypeDefinition(fromType(OutboundEndpoint.class))
        .withSetterParameterDefinition("connector", fromSimpleReferenceParameter("connector-ref").build())
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("encoding", fromSimpleParameter("encoding").build())
        .withSetterParameterDefinition("transactionConfig", fromChildConfiguration(TransactionConfig.class).build())
        .withSetterParameterDefinition("deleteUnacceptedMessages", fromSimpleParameter("deleteUnacceptedMessages").build())
        .withSetterParameterDefinition("initialState", fromSimpleParameter("initialState").build())
        .withSetterParameterDefinition("responseTimeout", fromSimpleParameter("responseTimeout").build())
        .withSetterParameterDefinition("retryPolicyTemplate", fromChildConfiguration(RetryPolicy.class).build())
        .withSetterParameterDefinition("exchangePattern", fromSimpleParameter("exchange-pattern").build())
        .withSetterParameterDefinition("muleContext", fromReferenceObject(MuleContext.class).build())
        .withSetterParameterDefinition("messageProcessors", fromChildCollectionConfiguration(MessageProcessor.class).build())
        .withSetterParameterDefinition("disableTransportTransformer", fromSimpleParameter("disableTransportTransformer").build())
        .withSetterParameterDefinition("mimeType", fromSimpleParameter("mimeType").build())
        .withSetterParameterDefinition("exchangePattern", fromSimpleParameter("exchange-pattern").build())
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("redeliveryPolicy", fromChildConfiguration(AbstractRedeliveryPolicy.class).build());
  }

  protected ComponentBuildingDefinition.Builder getEndpointBuildingDefinitionBuilder() {
    return baseDefinition.copy().withIdentifier(ENDPOINT_ELEMENT).withTypeDefinition(fromType(EndpointURIEndpointBuilder.class))
        .withSetterParameterDefinition("connector", fromSimpleReferenceParameter("connector-ref").build())
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("encoding", fromSimpleParameter("encoding").build())
        .withSetterParameterDefinition("transactionConfig", fromChildConfiguration(TransactionConfig.class).build())
        .withSetterParameterDefinition("deleteUnacceptedMessages", fromSimpleParameter("deleteUnacceptedMessages").build())
        .withSetterParameterDefinition("initialState", fromSimpleParameter("initialState").build())
        .withSetterParameterDefinition("responseTimeout", fromSimpleParameter("responseTimeout").build())
        .withSetterParameterDefinition("retryPolicyTemplate", fromChildConfiguration(RetryPolicy.class).build())
        .withSetterParameterDefinition("exchangePattern", fromSimpleParameter("exchange-pattern").build())
        .withSetterParameterDefinition("muleContext", fromReferenceObject(MuleContext.class).build())
        .withSetterParameterDefinition("messageProcessors", fromChildCollectionConfiguration(MessageProcessor.class).build())
        .withSetterParameterDefinition("disableTransportTransformer", fromSimpleParameter("disableTransportTransformer").build())
        .withSetterParameterDefinition("mimeType", fromSimpleParameter("mimeType").build())
        .withSetterParameterDefinition("redeliveryPolicy", fromChildConfiguration(AbstractRedeliveryPolicy.class).build())
        .withSetterParameterDefinition("properties", fromUndefinedSimpleAttributes().build())
        .withSetterParameterDefinition("exchangePattern", fromSimpleParameter("exchange-pattern").build())
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build()).asPrototype();
  }

  protected ComponentBuildingDefinition.Builder getInboundEndpointBuildingDefinitionBuilder() {
    return baseDefinition.copy().withIdentifier(INBOUND_ENDPOINT_ELEMENT).withObjectFactoryType(InboundEndpointFactoryBean.class)
        .withTypeDefinition(fromType(InboundEndpoint.class))
        .withSetterParameterDefinition("connector", fromSimpleReferenceParameter("connector-ref").build())
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("encoding", fromSimpleParameter("encoding").build())
        .withSetterParameterDefinition("transactionConfig", fromChildConfiguration(TransactionConfig.class).build())
        .withSetterParameterDefinition("deleteUnacceptedMessages", fromSimpleParameter("deleteUnacceptedMessages").build())
        .withSetterParameterDefinition("initialState", fromSimpleParameter("initialState").build())
        .withSetterParameterDefinition("responseTimeout", fromSimpleParameter("responseTimeout").build())
        .withSetterParameterDefinition("retryPolicyTemplate", fromChildConfiguration(RetryPolicy.class).build())
        .withSetterParameterDefinition("exchangePattern", fromSimpleParameter("exchange-pattern").build())
        .withSetterParameterDefinition("muleContext", fromReferenceObject(MuleContext.class).build())
        .withSetterParameterDefinition("messageProcessors", fromChildCollectionConfiguration(MessageProcessor.class).build())
        .withSetterParameterDefinition("disableTransportTransformer", fromSimpleParameter("disableTransportTransformer").build())
        .withSetterParameterDefinition("mimeType", fromSimpleParameter("mimeType").build())
        .withSetterParameterDefinition("redeliveryPolicy", fromChildConfiguration(AbstractRedeliveryPolicy.class).build())
        .withSetterParameterDefinition("exchangePattern", fromSimpleParameter("exchange-pattern").build())
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("properties", fromUndefinedSimpleAttributes().build());
  }

  protected ComponentBuildingDefinition.Builder getBaseTransactionDefinitionBuilder() {
    return baseDefinition.copy().withIdentifier("transaction").withTypeDefinition(fromType(MuleTransactionConfig.class))
        .withSetterParameterDefinition("timeout", fromSimpleParameter("timeout").build())
        .withSetterParameterDefinition("action", fromSimpleParameter("action", getTransactionActionTypeConverter()).build());
  }


  private TypeConverter<String, Byte> getTransactionActionTypeConverter() {
    return action -> {
      switch (action) {
        case "ALWAYS_BEGIN":
          return TransactionConfig.ACTION_ALWAYS_BEGIN;
        case "ALWAYS_JOIN":
          return TransactionConfig.ACTION_ALWAYS_JOIN;
        case "JOIN_IF_POSSIBLE":
          return TransactionConfig.ACTION_JOIN_IF_POSSIBLE;
        case "NONE":
          return TransactionConfig.ACTION_NONE;
        case "BEGIN_OR_JOIN":
          return TransactionConfig.ACTION_BEGIN_OR_JOIN;
        case "INDIFFERENT":
          return TransactionConfig.ACTION_INDIFFERENT;
        case "NEVER":
          return TransactionConfig.ACTION_NEVER;
        case "NOT_SUPPORTED":
          return TransactionConfig.ACTION_NOT_SUPPORTED;
        default:
          throw new MuleRuntimeException(createStaticMessage("Wrong transaction action configuration parameter: " + action));
      }
    };
  }
}
