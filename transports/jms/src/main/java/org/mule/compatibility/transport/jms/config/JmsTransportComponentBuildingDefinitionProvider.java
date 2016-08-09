/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.config;

import static org.mule.compatibility.transport.jms.config.JmsXmlNamespaceInfoProvider.JMS_NAMESPACE;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleReferenceParameter;
import static org.mule.runtime.config.spring.dsl.api.TypeDefinition.fromConfigurationAttribute;
import static org.mule.runtime.config.spring.dsl.api.TypeDefinition.fromType;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;

import org.mule.compatibility.config.spring.TransportComponentBuildingDefinitionProvider;
import org.mule.compatibility.transport.jms.JmsClientAcknowledgeTransactionFactory;
import org.mule.compatibility.transport.jms.JmsConnector;
import org.mule.compatibility.transport.jms.JmsTransactionFactory;
import org.mule.compatibility.transport.jms.activemq.ActiveMQJmsConnector;
import org.mule.compatibility.transport.jms.activemq.ActiveMQXAJmsConnector;
import org.mule.compatibility.transport.jms.filters.JmsSelectorFilter;
import org.mule.compatibility.transport.jms.jndi.JndiNameResolver;
import org.mule.compatibility.transport.jms.jndi.SimpleJndiNameResolver;
import org.mule.compatibility.transport.jms.transformers.JMSMessageToObject;
import org.mule.compatibility.transport.jms.transformers.ObjectToJMSMessage;
import org.mule.compatibility.transport.jms.weblogic.WeblogicJmsConnector;
import org.mule.compatibility.transport.jms.websphere.WebsphereJmsConnector;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.TypeConverter;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Session;

import org.springframework.jms.connection.CachingConnectionFactory;

/**
 * Provider for {@code ComponentBuildingDefinition}s to parse JMS transport configuration.
 *
 * @since 4.0
 */
public class JmsTransportComponentBuildingDefinitionProvider extends TransportComponentBuildingDefinitionProvider {

  private static final ComponentBuildingDefinition.Builder baseDefinition =
      new ComponentBuildingDefinition.Builder().withNamespace(JMS_NAMESPACE);

  @Override
  public void init(MuleContext muleContext) {
    super.init(muleContext);
  }


  private ComponentBuildingDefinition.Builder setJmsEndpointConfiguration(ComponentBuildingDefinition.Builder enpointBuilder) {
    return enpointBuilder.withNamespace(JMS_NAMESPACE)
        .withSetterParameterDefinition("selector", fromChildConfiguration(JmsSelectorFilter.class).build())
        .withIgnoredConfigurationParameter("queue").withIgnoredConfigurationParameter("topic");
  }

  @Override
  protected ComponentBuildingDefinition.Builder getOutboundEndpointBuildingDefinitionBuilder() {
    return setJmsEndpointConfiguration(super.getOutboundEndpointBuildingDefinitionBuilder());
  }

  @Override
  protected ComponentBuildingDefinition.Builder getEndpointBuildingDefinitionBuilder() {
    return setJmsEndpointConfiguration(super.getEndpointBuildingDefinitionBuilder());
  }

  @Override
  protected ComponentBuildingDefinition.Builder getInboundEndpointBuildingDefinitionBuilder() {
    return setJmsEndpointConfiguration(super.getInboundEndpointBuildingDefinitionBuilder());
  }

  @Override
  public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {
    List<ComponentBuildingDefinition> componentBuildingDefinitions = new ArrayList<>();
    ComponentBuildingDefinition.Builder baseJmsConnector = getBaseConnector().withTypeDefinition(fromType(JmsConnector.class))
        .withSetterParameterDefinition("acknowledgementMode",
                                       fromSimpleParameter("acknowledgementMode", getAckModeConverter()).build())
        .withSetterParameterDefinition("clientId", fromSimpleParameter("clientId").build())
        .withSetterParameterDefinition("durable", fromSimpleParameter("durable").build())
        .withSetterParameterDefinition("noLocal", fromSimpleParameter("noLocal").build())
        .withSetterParameterDefinition("persistentDelivery", fromSimpleParameter("persistentDelivery").build())
        .withSetterParameterDefinition("cacheJmsSessions", fromSimpleParameter("cacheJmsSessions").build())
        .withSetterParameterDefinition("eagerConsumer", fromSimpleParameter("eagerConsumer").build())
        .withSetterParameterDefinition("username", fromSimpleParameter("username").build())
        .withSetterParameterDefinition("password", fromSimpleParameter("password").build())
        .withSetterParameterDefinition("jndiDestinations", fromSimpleParameter("jndiDestinations").build())
        .withSetterParameterDefinition("jndiInitialFactory", fromSimpleParameter("jndiInitialFactory").build())
        .withSetterParameterDefinition("jndiProviderUrl", fromSimpleParameter("jndiProviderUrl").build())
        .withSetterParameterDefinition("connectionFactoryJndiName", fromSimpleParameter("connectionFactoryJndiName").build())
        .withSetterParameterDefinition("jndiProviderProperties",
                                       fromSimpleReferenceParameter("jndiProviderProperties-ref").build())
        .withSetterParameterDefinition("forceJndiDestinations", fromSimpleParameter("forceJndiDestinations").build())
        .withSetterParameterDefinition("specification", fromSimpleParameter("specification").build())
        .withSetterParameterDefinition("disableTemporaryReplyToDestinations",
                                       fromSimpleParameter("disableTemporaryReplyToDestinations").build())
        .withSetterParameterDefinition("returnOriginalMessageAsReply",
                                       fromSimpleParameter("returnOriginalMessageAsReply").build())
        .withSetterParameterDefinition("embeddedMode", fromSimpleParameter("embeddedMode").build())
        .withSetterParameterDefinition("honorQosHeaders", fromSimpleParameter("honorQosHeaders").build())
        .withSetterParameterDefinition("sameRMOverrideValue", fromSimpleParameter("sameRMOverrideValue").build())
        .withSetterParameterDefinition("maxRedelivery", fromSimpleParameter("maxRedelivery").build())
        .withSetterParameterDefinition("redeliveryHandlerFactory",
                                       fromSimpleReferenceParameter("redeliveryHandlerFactory-ref").build())
        .withSetterParameterDefinition("connectionFactory", fromSimpleReferenceParameter("connectionFactory-ref").build())
        .withSetterParameterDefinition("numberOfConsumers", fromSimpleParameter("numberOfConsumers").build())
        .withSetterParameterDefinition("numberOfConcurrentTransactedReceivers",
                                       fromSimpleParameter("numberOfConcurrentTransactedReceivers").build())
        .withSetterParameterDefinition("jndiNameResolver", fromChildConfiguration(JndiNameResolver.class).build());

    componentBuildingDefinitions.add(baseJmsConnector.copy().build());

    componentBuildingDefinitions.add(baseJmsConnector.copy().withIdentifier("custom-connector")
        .withTypeDefinition(fromConfigurationAttribute("class")).build());

    componentBuildingDefinitions.add(baseJmsConnector.copy().withIdentifier("weblogic-connector")
        .withTypeDefinition(fromType(WeblogicJmsConnector.class)).build());

    componentBuildingDefinitions.add(baseJmsConnector.copy().withIdentifier("websphere-connector")
        .withTypeDefinition(fromType(WebsphereJmsConnector.class)).build());


    ComponentBuildingDefinition.Builder baseActiveMqConnector =
        baseJmsConnector.copy().withSetterParameterDefinition("name", fromSimpleParameter("name").build())
            .withSetterParameterDefinition("brokerURL", fromSimpleParameter("brokerURL").build());

    componentBuildingDefinitions.add(baseActiveMqConnector.copy().withIdentifier("activemq-connector")
        .withTypeDefinition(fromType(ActiveMQJmsConnector.class)).build());

    componentBuildingDefinitions.add(baseActiveMqConnector.copy().withIdentifier("activemq-xa-connector")
        .withTypeDefinition(fromType(ActiveMQXAJmsConnector.class)).build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("default-jndi-name-resolver")
        .withTypeDefinition(fromType(SimpleJndiNameResolver.class))
        .withSetterParameterDefinition("jndiInitialFactory", fromSimpleParameter("jndiInitialFactory").build())
        .withSetterParameterDefinition("jndiProviderUrl", fromSimpleParameter("jndiProviderUrl").build())
        .withSetterParameterDefinition("jndiProviderProperties",
                                       fromSimpleReferenceParameter("jndiProviderProperties-ref").build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("custom-jndi-name-resolver")
        .withTypeDefinition(fromConfigurationAttribute("class")).build());

    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier("selector").withTypeDefinition(fromType(JmsSelectorFilter.class))
            .withSetterParameterDefinition("expression", fromSimpleParameter("expression").build()).build());

    componentBuildingDefinitions.add(getBaseTransactionDefinitionBuilder().copy().withNamespace(JMS_NAMESPACE)
        .withSetterParameterDefinition("factory", fromFixedValue(new JmsTransactionFactory()).build()).build());

    componentBuildingDefinitions.add(getBaseTransactionDefinitionBuilder().copy().withNamespace(JMS_NAMESPACE)
        .withIdentifier("client-ack-transaction")
        .withSetterParameterDefinition("factory", fromFixedValue(new JmsClientAcknowledgeTransactionFactory()).build()).build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("jmsmessage-to-object-transformer")
        .withTypeDefinition(fromType(JMSMessageToObject.class)).build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("object-to-jmsmessage-transformer")
        .withTypeDefinition(fromType(ObjectToJMSMessage.class)).build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("caching-connection-factory")
        .withTypeDefinition(fromType(CachingConnectionFactory.class))
        .withObjectFactoryType(CachingConnectionFactoryFactoryBean.class)
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("sessionCacheSize", fromSimpleParameter("sessionCacheSize").build())
        .withSetterParameterDefinition("cacheProducers", fromSimpleParameter("cacheProducers").build())
        .withSetterParameterDefinition("username", fromSimpleParameter("username").build())
        .withSetterParameterDefinition("password", fromSimpleParameter("password").build())
        .withSetterParameterDefinition("connectionFactory", fromSimpleReferenceParameter("connectionFactory-ref").build())
        .build());

    componentBuildingDefinitions.add(getEndpointBuildingDefinitionBuilder().build());
    componentBuildingDefinitions.add(getInboundEndpointBuildingDefinitionBuilder().build());
    componentBuildingDefinitions.add(getOutboundEndpointBuildingDefinitionBuilder().build());
    return componentBuildingDefinitions;
  }


  private TypeConverter<String, Integer> getAckModeConverter() {
    return new TypeConverter<String, Integer>() {

      @Override
      public Integer convert(String ackMode) {
        switch (ackMode) {
          case "AUTO_ACKNOWLEDGE":
            return Session.AUTO_ACKNOWLEDGE;
          case "CLIENT_ACKNOWLEDGE":
            return Session.CLIENT_ACKNOWLEDGE;
          case "DUPS_OK_ACKNOWLEDGE":
            return Session.DUPS_OK_ACKNOWLEDGE;
          default:
            throw new MuleRuntimeException(createStaticMessage("Wrong acknowledgement mode configuration: " + ackMode));
        }
      }
    };
  }

}
