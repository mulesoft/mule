/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metadata.api.cache;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.metadata.api.locator.ComponentLocator;

/**
 * Factory definition to create {@link MetadataCacheIdGenerator}s isolated from the application context in which it will be used.
 *
 * @since 4.1.4, 4.2.0
 */
public interface MetadataCacheIdGeneratorFactory<T> {

  String METADATA_CACHE_ID_GENERATOR_KEY = "metadata.cache.id.model.generator.factory";

  /**
   * Creates a new {@link MetadataCacheIdGenerator} valid for the given context information provided.
   *
   * @param context the {@link DslResolvingContext} in which the generator will be used.
   * @param locator a {@link ComponentLocator} that allows the generator to locate any component in the current context.
   * @return a new {@link MetadataCacheIdGenerator} valid for the given context.
   */
  MetadataCacheIdGenerator<T> create(DslResolvingContext context, ComponentLocator<T> locator);

}
