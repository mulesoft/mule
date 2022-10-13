/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metadata.api.generation;

import org.mule.runtime.metadata.internal.generation.ComponentParameterizationBasedMetadataCacheIdGenerator;

/**
 * A Factory of {@link ComponentParameterizationMetadataCacheIdGenerator}
 *
 * @since 4.5
 */
public class ComponentParameterizationMetadataCacheIdGeneratorFactory {

  /**
   * @return a new instance of an implementation of {@link ComponentParameterizationMetadataCacheIdGenerator}
   */
  public ComponentParameterizationMetadataCacheIdGenerator create() {
    return new ComponentParameterizationBasedMetadataCacheIdGenerator();
  }
}
