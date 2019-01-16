/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.metadata.cache;

import java.util.Optional;

/**
 * Provides a way to generate the {@link MetadataCacheId} of a given representation of a Component.
 * The generated ID will take into account different elements of the Component configuration,
 * depending on what's the associated element being cached with this ID.
 * <p>
 * In particular, different rules apply for the MetadataTypes resolution than those used for the MetadataKeys resolution.
 *
 * @since 4.1.4, 4.2.0
 */
public interface MetadataCacheIdGenerator<T> {


  /**
   * Calculates the {@link MetadataCacheId} required to identify the MetadataType associated to the output of the given
   * {@code component}. This method will take into account the values of the configured
   * {@link org.mule.runtime.api.metadata.MetadataKey} to provide an unique identifier of the {@code component} output type
   * definition.
   *
   * @param component the configured component
   * @return a {@link MetadataCacheId} that identifies the component output type with all its current configuration or
   *         {@link Optional#empty} if no valid identifier can be created.
   */
  Optional<MetadataCacheId> getIdForComponentOutputMetadata(T component);

  /**
   * Calculates the {@link MetadataCacheId} required to identify the MetadataType associated to the attributes of the given
   * {@code component}. This method will take into account the values of the configured
   * {@link org.mule.runtime.api.metadata.MetadataKey} to provide an unique identifier of the {@code component} attributes type
   * definition.
   *
   * @param component the configured component
   * @return a {@link MetadataCacheId} that identifies the component attributes type with all its current configuration or
   *         {@link Optional#empty} if no valid identifier can be created.
   */
  Optional<MetadataCacheId> getIdForComponentAttributesMetadata(T component);

  /**
   * Calculates the {@link MetadataCacheId} required to identify the MetadataType associated to the parameter named
   * {@code parameterName} of the given {@code component}. This method will take into account the values of the configured
   * {@link org.mule.runtime.api.metadata.MetadataKey} to provide an unique identifier of the {@code component} attributes type
   * definition.
   *
   * @param component the configured component
   * @param parameterName the name of the parameter
   * @return a {@link MetadataCacheId} that identifies the component's parameter type with all its current configuration or
   *         {@link Optional#empty} if no valid identifier can be created.
   */
  Optional<MetadataCacheId> getIdForComponentInputMetadata(T component, String parameterName);

  /**
   * Calculates the {@link MetadataCacheId} required to identify the MetadataTypes associated to the given {@code component}. This
   * method will take into account the values of the configured {@link org.mule.runtime.api.metadata.MetadataKey} to provide an
   * unique identifier of the {@code component}'s type definitions.
   *
   * @param component the configured component
   * @return a {@link MetadataCacheId} that identifies the component Types with all its current configuration or
   * {@link Optional#empty} if no valid identifier can be created for the component.
   */
  Optional<MetadataCacheId> getIdForComponentMetadata(T component);

  /**
   * Calculates the {@link MetadataCacheId} required to identify the {@link org.mule.runtime.api.metadata.MetadataKey}s
   * associated to the given {@code component}.
   * This method will ignore the values of the configured MetadataKeys as long as they don't affect the resolution of nested keys.
   *
   * @param component the configured component
   * @return a {@link MetadataCacheId} that identifies the MetadataKeys associated to the component with its current configuration,
   * or {@link Optional#empty} if no valid identifier can be created for the component.
   */
  Optional<MetadataCacheId> getIdForMetadataKeys(T component);

  /**
   * Calculates the {@link MetadataCacheId} required to identify all the Metadata associated to the given {@code component},
   * and its siblings, based on the referenced global element configuration.
   * This method will ignore the values of the configured MetadataKeys as long as they don't affect the resolution of nested keys.
   *
   * @param component the configured component
   * @return a {@link MetadataCacheId} that identifies the global Metadata associated to the component with its current configuration,
   * or {@link Optional#empty} if no valid identifier can be created for the component.
   */
  Optional<MetadataCacheId> getIdForGlobalMetadata(T component);

}
