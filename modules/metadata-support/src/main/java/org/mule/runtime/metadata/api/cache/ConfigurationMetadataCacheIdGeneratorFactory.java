/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
