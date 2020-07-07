/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata.context;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;

import java.util.Map;
import java.util.Optional;

/**
 * Context for resolving the {@link org.mule.runtime.core.internal.value.cache.ValueProviderCacheId} for a set of values
 * already retrieved from a {@link org.mule.runtime.extension.api.values.ValueProvider}
 *
 * @param <P> The type used to represent the values for the configured parameters in this context
 *
 * @since 4.4.0
 */
public interface ValueProviderCacheIdGeneratorContext<P> {

  /**
   * Get the {@link ComponentIdentifier} from the component that owns the parameter for which the values were resolved
   *
   * @return a {@link ComponentIdentifier} associated with the resolved parameter owner.
   */
  ComponentIdentifier getOwnerId();

  /**
   * Get the {@link ParameterizedModel} of the component that owns the parameter for which values were resolved.
   *
   * @return the {@link ParameterizedModel} of the resolved parameter's owner.
   */
  ParameterizedModel getOwnerModel();

  /**
   * Return a {@link Map<String, ParameterInfo<P>>} with the configured parameter values for the context that also owns
   * the resolved parameter.
   *
   * @return a {@link Map<String, ParameterInfo<P>>} with the parameter values
   */
  Map<String, ParameterInfo<P>> getParameters();

  /**
   * Get a {@link ValueProviderCacheIdGeneratorContext} that represents the configuration referenced by the component
   * owning the resolved parameter, in case it exists.
   * </p>
   * Cases in which this should return {@link Optional#empty()} could be:
   *  - If this {@link ValueProviderCacheIdGeneratorContext} already represents a configuration or a connection.
   *  - This {@link ValueProviderCacheIdGeneratorContext} represents an Operation or Source that do not require a config
   *
   * @return an {@link Optional<ValueProviderCacheIdGeneratorContext>} that represents the configuration of this component,
   * in case it exists.
   */
  Optional<ValueProviderCacheIdGeneratorContext<P>> getConfigContext();

  /**
   * Get a {@link ValueProviderCacheIdGeneratorContext} that represents the connection referenced by the component
   * owning the resolved parameter, in case it exists.
   * </p>
   * Cases in which this should return {@link Optional#empty()} could be:
   *  - If this {@link ValueProviderCacheIdGeneratorContext} already represents a configuration or a connection.
   *  - This {@link ValueProviderCacheIdGeneratorContext} represents an Operation or Source that do not require a connection
   *
   * @return an {@link Optional<ValueProviderCacheIdGeneratorContext>} that represents the connection of this component,
   * in case it exists.
   */
  Optional<ValueProviderCacheIdGeneratorContext<P>> getConnectionContext();

  /**
   * If this {@link ValueProviderCacheIdGeneratorContext} represents a Component (Operation, Source) or not (Connection, Configuration)
   * @return {@link Boolean#TRUE} if this {@link ValueProviderCacheIdGeneratorContext} represents a Component.
   */
  default boolean isForComponent() {
    return getOwnerModel() instanceof ComponentModel;
  }

  /**
   * A value holder for parameters that are already configured in a given {@link ValueProviderCacheIdGeneratorContext}.
   * @param <T> The type used to represent the parameter values
   */
  interface ParameterInfo<T> {

    /**
     * Get the parameter name
     *
     * @return the parameter name
     */
    String getName();

    /**
     * Get the hash value computed from the parameter value.
     * </p>
     * This is intended to allow implementations of this interface to define custom behaviour for hashing complex parameters.
     * There may be cases where a simple {@link Object#hashCode()} is not enough.
     *
     * @return an {@link Integer} that results from hashing the value of this parameter.
     */
    int getHashValue();

    /**
     * Get the actual value fo the parameter.
     *
     * @return the value of the parameter.
     */
    T getValue();

  }

}
