/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.config;

import static org.mule.runtime.config.spring.dsl.model.CoreComponentBuildingDefinitionProvider.getTransformerBaseBuilder;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleReferenceParameter;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromTextContent;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import org.mule.plugin.scripting.component.GroovyRefreshableBeanBuilder;
import org.mule.plugin.scripting.component.ScriptComponent;
import org.mule.plugin.scripting.component.Scriptable;
import org.mule.plugin.scripting.component.ScriptingProperty;
import org.mule.plugin.scripting.filter.ScriptFilter;
import org.mule.plugin.scripting.transformer.ScriptTransformer;
import org.mule.runtime.core.api.interceptor.Interceptor;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair;

import java.util.LinkedList;
import java.util.List;

/**
 * Provider of {@link ComponentBuildingDefinition} for Scripting module components.
 *
 * @since 4.0
 */
public class ScriptingComponentBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider {

  public static final String SCRIPTING_NAMESPACE = "scripting";

  private static final String TEXT = "text";
  private static final String SCRIPT = "script";
  private static final String COMPONENT = "component";
  private static final String PROPERTY = "property";
  private static final String TRANSFORMER = "transformer";
  private static final String FILTER = "filter";
  private static final String GROOVY_REFRESHABLE = "groovy-refreshable";

  private static ComponentBuildingDefinition.Builder baseDefinition =
      new ComponentBuildingDefinition.Builder().withNamespace(SCRIPTING_NAMESPACE);

  @Override
  public void init() {}

  @Override
  public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {
    List<ComponentBuildingDefinition> componentBuildingDefinitions = new LinkedList<>();

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(TEXT)
        .withTypeDefinition(fromType(String.class))
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(PROPERTY)
        .withTypeDefinition(fromType(ScriptingProperty.class))
        .withConstructorParameterDefinition(fromSimpleParameter("key").build())
        .withSetterParameterDefinition("value", fromSimpleParameter("value").build())
        .withSetterParameterDefinition("value", fromSimpleReferenceParameter("value-ref").build())
        .build());

    //TODO: MULE-11960 - scriptFile and scriptText are mutually exclusive
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(SCRIPT)
        .withTypeDefinition(fromType(Scriptable.class))
        .withIgnoredConfigurationParameter("name")
        .withSetterParameterDefinition("scriptEngineName", fromSimpleParameter("engine").build())
        .withSetterParameterDefinition("scriptFile", fromSimpleParameter("file").build())
        .withSetterParameterDefinition("scriptText", fromTextContent().build())
        .withSetterParameterDefinition("scriptText", fromChildConfiguration(String.class).withIdentifier("text").build())
        .withSetterParameterDefinition("properties", fromChildCollectionConfiguration(ScriptingProperty.class).build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(COMPONENT)
        .withTypeDefinition(fromType(ScriptComponent.class))
        .withSetterParameterDefinition(SCRIPT, fromChildConfiguration(Scriptable.class).build())
        .withSetterParameterDefinition(SCRIPT, fromSimpleReferenceParameter("script-ref").build())
        .withSetterParameterDefinition("interceptors", fromChildCollectionConfiguration(Interceptor.class).build())
        .build());

    componentBuildingDefinitions.add(getTransformerBaseBuilder(ScriptTransformer.class,
                                                               KeyAttributeDefinitionPair
                                                                   .newBuilder()
                                                                   .withKey(SCRIPT)
                                                                   .withAttributeDefinition(fromChildConfiguration(Scriptable.class)
                                                                       .build())
                                                                   .build())
                                                                       .withSetterParameterDefinition("commonConfiguratorType",
                                                                                                      fromFixedValue(ScriptingTransformerConfigurator.class)
                                                                                                          .build())
                                                                       .withIdentifier(TRANSFORMER)
                                                                       .withNamespace(SCRIPTING_NAMESPACE)
                                                                       .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(FILTER)
        .withTypeDefinition(fromType(ScriptFilter.class))
        .withIgnoredConfigurationParameter("name")
        .withSetterParameterDefinition(SCRIPT, fromChildConfiguration(Scriptable.class).build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(GROOVY_REFRESHABLE)
        .withTypeDefinition(fromType(GroovyRefreshableBeanBuilder.class))
        .withIgnoredConfigurationParameter("name")
        .withSetterParameterDefinition("methodName", fromSimpleParameter("methodName").build())
        .withSetterParameterDefinition("refreshableBean", fromSimpleReferenceParameter("refreshableBean-ref").build())
        .build());

    return componentBuildingDefinitions;
  }
}
