/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metadata.api.cache;

import org.mule.runtime.metadata.internal.cache.AstConfigurationMetadataCacheIdGenerator;

/**
 * A Factory of {@link ConfigurationMetadataCacheIdGenerator}
 *
 * @since 4.5
 */
public class ConfigurationMetadataCacheIdGeneratorFactory {

  /**
   * @return a new instance of an implementation of {@link ConfigurationMetadataCacheIdGenerator}
   */
  public ConfigurationMetadataCacheIdGenerator create() {
    return new AstConfigurationMetadataCacheIdGenerator();
  }
}
