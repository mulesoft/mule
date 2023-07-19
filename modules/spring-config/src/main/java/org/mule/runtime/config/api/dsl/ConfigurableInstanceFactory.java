/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.api.dsl;

import org.mule.runtime.dsl.api.component.AttributeDefinition;

import java.util.Map;

/**
 * Factory for the instances created by the {@link ConfigurableObjectFactory}.
 * <p>
 * Implementations of this interfaces must be injected into {@link ConfigurableObjectFactory} by using a
 * {@link AttributeDefinition.Builder#fromFixedValue(Object)} when declaring the
 * {@link org.mule.runtime.dsl.api.component.ComponentBuildingDefinition}.
 *
 * @param <T> type of the object to be created
 * @since 4.0
 */
public interface ConfigurableInstanceFactory<T> {

  /**
   * Creates the object to be used at runtime.
   *
   * @param parameters the set of configuration parameters according to the
   *                   {@link org.mule.runtime.dsl.api.component.ComponentBuildingDefinition} configuration.
   * @return the object to be used at runtime.
   */
  T createInstance(Map<String, Object> parameters);

}
