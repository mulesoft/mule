/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.value.cache;

import java.util.Optional;

/**
 * Provides a way to generate the {@link ValueProviderCacheId} to identify uniquely a set of resolved value for an specific
 * parameter. The generated ID will take into account different elements of the Component configuration, needed by the
 * {@link org.mule.runtime.extension.api.values.ValueProvider} to resolve the values.
 *
 * @since 4.2.3, 4.3.0
 */
public interface ValueProviderCacheIdGenerator<T> {

  /**
   * Calculates a {@link ValueProviderCacheId} required to identify all the values returned by a
   * {@link org.mule.runtime.extension.api.values.ValueProvider} associated with the parameter in the given component.
   *
   * @param containerComponent the component that holds the parameter
   * @param parameterName      the name of the parameter which values are provided by a
   *                           {@link org.mule.runtime.extension.api.values.ValueProvider}
   * @return a {@see Optional<ValueProviderCacheId>} with the resolved id in case it's possible, {@link Optional#empty()}
   *         otherwise.
   */
  Optional<ValueProviderCacheId> getIdForResolvedValues(T containerComponent, String parameterName);

  /**
   * Calculates a {@link ValueProviderCacheId} required to identify all the values returned by a
   * {@link org.mule.runtime.extension.api.values.ValueProvider} associated with the field contained by the parameter in the given
   * container.
   *
   * Keep in mind that if there is no {@link org.mule.runtime.api.meta.model.parameter.FieldValueProviderModel} associated with
   * the given targetPath in the parameter, this method should return {@link Optional#empty()}
   *
   * @param containerComponent the component that holds the parameter
   * @param parameterName      the name of the parameter containing the field for which {@link org.mule.runtime.api.value.Value}s
   *                           are provided.
   * @param targetPath         the path to locate the field with a {@link org.mule.sdk.api.values.ValueProvider} within the
   *                           parameter in the container component.
   *
   * @return a {@see Optional<ValueProviderCacheId>} with the resolved id in case it's possible, {@link Optional#empty()}
   *         otherwise.
   */
  Optional<ValueProviderCacheId> getIdForResolvedValues(T containerComponent, String parameterName, String targetPath);

}
