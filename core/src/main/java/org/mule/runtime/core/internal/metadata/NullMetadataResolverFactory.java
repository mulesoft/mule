/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.metadata;

import org.mule.runtime.api.metadata.resolving.MetadataContentResolver;
import org.mule.runtime.api.metadata.resolving.MetadataKeysResolver;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;
import org.mule.runtime.api.metadata.resolving.MetadataAttributesResolver;
import org.mule.runtime.extension.api.introspection.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.introspection.metadata.NullMetadataResolver;

/**
 * Null implementation of a {@link MetadataResolverFactory}, which returns a {@link NullMetadataResolver} for every resolver
 * provided by the factory
 *
 * @since 4.0
 */
public class NullMetadataResolverFactory implements MetadataResolverFactory {

  private final NullMetadataResolver metadataResolver;

  public NullMetadataResolverFactory() {
    this.metadataResolver = new NullMetadataResolver();
  }

  /**
   * @return a {@link NullMetadataResolver} implementation of {@link MetadataKeysResolver}
   */
  @Override
  public MetadataKeysResolver getKeyResolver() {
    return metadataResolver;
  }

  /**
   * @return a {@link NullMetadataResolver} implementation of {@link MetadataContentResolver}
   */
  @Override
  public <T> MetadataContentResolver<T> getContentResolver() {
    return (MetadataContentResolver<T>) metadataResolver;
  }

  /**
   * @return a {@link NullMetadataResolver} implementation of {@link MetadataOutputResolver}
   */
  @Override
  public <T> MetadataOutputResolver<T> getOutputResolver() {
    return (MetadataOutputResolver<T>) metadataResolver;
  }

  @Override
  public <T> MetadataAttributesResolver<T> getOutputAttributesResolver() {
    return (MetadataAttributesResolver<T>) metadataResolver;
  }

}
