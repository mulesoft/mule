/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.spring.security.config;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleReferenceParameter;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import static org.mule.runtime.module.spring.security.config.MuleSecurityXmlNamespaceInfoProvider.MULE_SS_NAMESPACE;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.security.SecurityProvider;
import org.mule.runtime.core.processor.SecurityFilterMessageProcessor;
import org.mule.runtime.core.security.MuleSecurityManagerConfigurator;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.module.spring.security.SpringProviderAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ComponentBuildingDefinition} definitions for the components provided by the mule
 * spring security module.
 *
 * @since 4.0
 */
public class MuleSecurityComponentBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider {

  @Override
  public void init() {}

  @Override
  public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {
    List<ComponentBuildingDefinition> componentBuildingDefinitions = new ArrayList<>();
    ComponentBuildingDefinition.Builder baseDefinition =
        new ComponentBuildingDefinition.Builder().withNamespace(MULE_SS_NAMESPACE);

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("security-manager")
        .withTypeDefinition(fromType(SecurityManager.class)).withObjectFactoryType(MuleSecurityManagerConfigurator.class)
        .withSetterParameterDefinition("muleContext", fromReferenceObject(MuleContext.class).build())
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("providers", fromChildCollectionConfiguration(SecurityProvider.class).build()).build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("delegate-security-provider")
        .withTypeDefinition(fromType(SpringProviderAdapter.class))
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("securityProperties", fromChildCollectionConfiguration(SecurityProperty.class).build())
        .withSetterParameterDefinition("delegate", fromSimpleReferenceParameter("delegate-ref").build())
        .withSetterParameterDefinition("authenticationProvider",
                                       fromSimpleReferenceParameter("authenticationProvider-ref").build())
        .build());

    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier("security-property").withTypeDefinition(fromType(SecurityProperty.class))
            .withConstructorParameterDefinition(fromSimpleParameter("name").build())
            .withConstructorParameterDefinition(fromSimpleParameter("value").build()).build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("authorization-filter")
        .withTypeDefinition(fromType(SecurityFilterMessageProcessor.class))
        .withObjectFactoryType(AuthorizationFilterObjectFactory.class)
        .withConstructorParameterDefinition(fromSimpleParameter("requiredAuthorities",
                                                                (value) -> asList(((String) value).split(",")).stream()
                                                                    .map(String::trim).collect(toList())).build())
        .build());
    return componentBuildingDefinitions;
  }
}
