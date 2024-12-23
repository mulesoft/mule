/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model;

import static org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry.WrapperElementType.COLLECTION;
import static org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry.WrapperElementType.MAP;
import static org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry.WrapperElementType.SINGLE;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.config.api.dsl.processor.AbstractAttributeDefinitionVisitor;
import org.mule.runtime.dsl.api.component.AttributeDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair;
import org.mule.runtime.dsl.api.component.SetterAttributeDefinition;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Registry with all {@link ComponentBuildingDefinition} that where discovered in the classpath.
 * <p/>
 * {@code ComponentBuildingDefinition}s are located using SPI class {@link ComponentBuildingDefinitionProvider}.
 *
 * @since 4.0
 * @deprecated since 4.4, use the SDK instead of registering parsers manually.
 */
@Deprecated
public final class ComponentBuildingDefinitionRegistry {

  private final Map<ComponentIdentifier, Deque<ComponentBuildingDefinition<?>>> builderDefinitionsMap = new HashMap<>();
  private final Map<String, WrapperElementType> wrapperIdentifierAndTypeMap = new HashMap<>();

  /**
   * Adds a new {@code ComponentBuildingDefinition} to the registry.
   *
   * @param builderDefinition definition to be added in the registry
   */
  public void register(ComponentBuildingDefinition<?> builderDefinition) {
    builderDefinitionsMap.computeIfAbsent(builderDefinition.getComponentIdentifier(),
                                          // Use a stack structure so the order is consistent across executions
                                          // and keep the behavior that the last element to be added takes precedence
                                          k -> new ArrayDeque<>())
        .push(builderDefinition);
    wrapperIdentifierAndTypeMap.putAll(getWrapperIdentifierAndTypeMap(builderDefinition));
  }

  /**
   * Lookups a {@code ComponentBuildingDefinition} for a certain configuration component.
   *
   * @param identifier the component identifier
   * @return the definition to build the component
   */
  public Optional<ComponentBuildingDefinition<?>> getBuildingDefinition(ComponentIdentifier identifier) {
    final Deque<ComponentBuildingDefinition<?>> definitions = builderDefinitionsMap.get(identifier);
    return definitions == null
        ? empty()
        : ofNullable(definitions.peek());
  }

  /**
   * Lookups a {@code ComponentBuildingDefinition} for a certain configuration component and a certain condition.
   *
   * @param identifier the component identifier
   * @param condition  how to determine which of the available definitions to use
   * @return the definition to build the component
   */
  public Optional<ComponentBuildingDefinition<?>> getBuildingDefinition(ComponentIdentifier identifier,
                                                                        Predicate<ComponentBuildingDefinition<?>> condition) {
    Collection<ComponentBuildingDefinition<?>> buildingDefinitions = builderDefinitionsMap.get(identifier);
    if (buildingDefinitions == null) {
      buildingDefinitions = emptyList();
    }

    return buildingDefinitions
        .stream()
        .filter(condition)
        .findFirst();
  }

  /**
   * Lookups a {@link WrapperElementType} for a certain configuration element.
   *
   * @param wrapperIdentifier the wrapper component identifier
   * @return the element type of the wrapper component
   */
  public Optional<WrapperElementType> getWrappedComponent(ComponentIdentifier wrapperIdentifier) {
    return ofNullable(wrapperIdentifierAndTypeMap.get(wrapperIdentifier.toString()));
  }

  private <T> Map<String, WrapperElementType> getWrapperIdentifierAndTypeMap(ComponentBuildingDefinition<T> buildingDefinition) {
    final Map<String, WrapperElementType> wrapperIdentifierAndTypeMap = new HashMap<>();
    AbstractAttributeDefinitionVisitor wrapperIdentifiersCollector = new AbstractAttributeDefinitionVisitor() {

      @Override
      public void onComplexChildCollection(Class<?> type, Optional<String> wrapperIdentifierOptional) {
        wrapperIdentifierOptional.ifPresent(wrapperIdentifier -> wrapperIdentifierAndTypeMap
            .put(abbreviateIdentifier(buildingDefinition, wrapperIdentifier), COLLECTION));
      }

      @Override
      public void onComplexChild(Class<?> type, Optional<String> wrapperIdentifierOptional, Optional<String> childIdentifier) {
        wrapperIdentifierOptional.ifPresent(wrapperIdentifier -> wrapperIdentifierAndTypeMap
            .put(abbreviateIdentifier(buildingDefinition, wrapperIdentifier), SINGLE));
      }

      @Override
      public void onComplexChildMap(Class<?> keyType, Class<?> valueType, String wrapperIdentifier) {
        wrapperIdentifierAndTypeMap.put(abbreviateIdentifier(buildingDefinition, wrapperIdentifier), MAP);
      }

      private <T> String abbreviateIdentifier(ComponentBuildingDefinition<T> buildingDefinition, String wrapperIdentifier) {
        final String namespace = buildingDefinition.getComponentIdentifier().getNamespace();
        if (CORE_PREFIX.equals(namespace)) {
          return wrapperIdentifier;
        } else {
          return namespace + ":" + wrapperIdentifier;
        }
      }

      @Override
      public void onMultipleValues(KeyAttributeDefinitionPair[] definitions) {
        for (KeyAttributeDefinitionPair attributeDefinition : definitions) {
          attributeDefinition.getAttributeDefinition().accept(this);
        }
      }
    };

    Consumer<AttributeDefinition> collectWrappersConsumer =
        attributeDefinition -> attributeDefinition.accept(wrapperIdentifiersCollector);
    buildingDefinition.getSetterParameterDefinitions().stream()
        .map(SetterAttributeDefinition::getAttributeDefinition)
        .forEach(collectWrappersConsumer);
    buildingDefinition.getConstructorAttributeDefinition().stream().forEach(collectWrappersConsumer);
    return wrapperIdentifierAndTypeMap;
  }

  /**
   * Types of wrapper elements in the XML config.
   */
  public enum WrapperElementType {
    SINGLE, COLLECTION, MAP
  }
}
