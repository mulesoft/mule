/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metadata.api.cache;

import org.mule.runtime.metadata.internal.cache.ComponentParameterizationBasedMetadataCacheIdGenerator;

/**
 * A Factory of {@link ComponentParameterizationMetadataCacheIdGenerator}
 *
 * @since 4.5
 */
public class ComponentParameterizationMetadataCacheIdGeneratorFactory {

  /**
   * @return a new instance of an implementation of {@link ComponentParameterizationMetadataCacheIdGenerator}
   */
  public ComponentParameterizationMetadataCacheIdGenerator create(ConfigurationMetadataCacheIdGenerator configIdGenerator) {
    return new ComponentParameterizationBasedMetadataCacheIdGenerator(configIdGenerator);
  }
}
