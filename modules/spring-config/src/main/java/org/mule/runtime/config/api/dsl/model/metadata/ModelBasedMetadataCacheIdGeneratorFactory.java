/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGenerator;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGeneratorFactory;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;


/**
 * A {@link ComponentModel} based implementation of a {@link MetadataCacheIdGeneratorFactory}
 *
 * @since 4.1.4, 4.2.0
 */
public class ModelBasedMetadataCacheIdGeneratorFactory implements MetadataCacheIdGeneratorFactory<ComponentConfiguration> {

  @Override
  public MetadataCacheIdGenerator<ComponentConfiguration> create(DslResolvingContext context,
                                                                 ComponentLocator<ComponentConfiguration> locator) {
    return new ComponentBasedMetadataCacheIdGenerator(context, locator);
  }
}
