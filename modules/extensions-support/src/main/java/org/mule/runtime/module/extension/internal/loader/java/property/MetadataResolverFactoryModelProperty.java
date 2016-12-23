/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;

/**
 * A model property which allows the enriched component to specify its
 * own {@link MetadataResolverFactory}
 *
 * @since 4.0
 */
public final class MetadataResolverFactoryModelProperty implements ModelProperty {

  private final MetadataResolverFactory metadataResolverFactory;

  /**
   * Creates a new instance
   *
   * @param metadataResolverFactory a {@link MetadataResolverFactory}
   */
  public MetadataResolverFactoryModelProperty(MetadataResolverFactory metadataResolverFactory) {
    this.metadataResolverFactory = metadataResolverFactory;
  }

  /**
   * @return a {@link MetadataResolverFactory}
   */
  public MetadataResolverFactory getMetadataResolverFactory() {
    return metadataResolverFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "metadataResolverFactory";
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
