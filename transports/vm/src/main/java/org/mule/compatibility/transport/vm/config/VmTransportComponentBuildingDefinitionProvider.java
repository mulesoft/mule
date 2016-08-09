/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm.config;

import static org.mule.compatibility.transport.vm.config.VmXmlNamespaceInfoProvider.VM_TRANSPORT_NAMESPACE;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.config.spring.dsl.api.TypeDefinition.fromType;

import org.mule.compatibility.config.spring.TransportComponentBuildingDefinitionProvider;
import org.mule.compatibility.transport.vm.VMConnector;
import org.mule.compatibility.transport.vm.VMTransactionFactory;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.factories.QueueProfileFactoryBean;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.store.ObjectStore;
import org.mule.runtime.core.config.QueueProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider for {@code ComponentBuildingDefinition}s to parse VM transport configuration.
 *
 * @since 4.0
 */
public class VmTransportComponentBuildingDefinitionProvider extends TransportComponentBuildingDefinitionProvider {

  private ComponentBuildingDefinition.Builder baseDefinition;

  @Override
  public void init(MuleContext muleContext) {
    baseDefinition = new ComponentBuildingDefinition.Builder().withNamespace(VM_TRANSPORT_NAMESPACE);
  }

  @Override
  public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {
    List<ComponentBuildingDefinition> componentBuildingDefinitions = new ArrayList<>();
    componentBuildingDefinitions
        .add(getOutboundEndpointBuildingDefinitionBuilder().withNamespace(VM_TRANSPORT_NAMESPACE).build());
    componentBuildingDefinitions.add(getInboundEndpointBuildingDefinitionBuilder().withNamespace(VM_TRANSPORT_NAMESPACE).build());
    componentBuildingDefinitions.add(getEndpointBuildingDefinitionBuilder().withNamespace(VM_TRANSPORT_NAMESPACE).build());
    componentBuildingDefinitions.add(getBaseTransactionDefinitionBuilder().withNamespace(VM_TRANSPORT_NAMESPACE)
        .withSetterParameterDefinition("factory", fromFixedValue(new VMTransactionFactory()).build()).build());
    componentBuildingDefinitions
        .add(getBaseConnector().withTypeDefinition(fromType(VMConnector.class)).withNamespace(VM_TRANSPORT_NAMESPACE)
            .withSetterParameterDefinition("queueProfile", fromChildConfiguration(QueueProfile.class).build())
            .withSetterParameterDefinition("queueTimeout", fromSimpleParameter("queueTimeout").build()).build());

    ComponentBuildingDefinition.Builder baseQueueProfileBuilder = baseDefinition.copy()
        .withTypeDefinition(fromType(QueueProfile.class)).withObjectFactoryType(QueueProfileFactoryBean.class)
        .withSetterParameterDefinition("maxOutstandingMessages", fromSimpleParameter("maxOutstandingMessages").build())
        .withSetterParameterDefinition("muleContext", fromReferenceObject(MuleContext.class).build())
        .withSetterParameterDefinition("queueStore", fromChildConfiguration(ObjectStore.class).build());
    componentBuildingDefinitions.add(baseQueueProfileBuilder.copy().withIdentifier("queueProfile").build());
    componentBuildingDefinitions.add(baseQueueProfileBuilder.copy().withIdentifier("queue-profile").build());

    return componentBuildingDefinitions;
  }
}
