/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.ClassUtils.getClassName;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.api.metadata.resolving.MetadataAttributesResolver;
import org.mule.runtime.api.metadata.resolving.MetadataContentResolver;
import org.mule.runtime.api.metadata.resolving.MetadataKeysResolver;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;
import org.mule.runtime.api.metadata.resolving.QueryEntityResolver;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.extension.api.annotation.Query;
import org.mule.runtime.extension.api.introspection.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.introspection.metadata.NullMetadataResolver;


/**
 * A {@link MetadataResolverFactory} implementation for {@link Query} operations, it provides initialized instances of
 * {@link MetadataOutputResolver} and {@link QueryEntityResolver}.
 *
 * {@link MetadataAttributesResolver}, {@link MetadataContentResolver} and {@link MetadataKeysResolver} returned instances are
 * always instances of {@link NullMetadataResolver}.
 *
 * @since 4.0
 */
public final class QueryMetadataResolverFactory implements MetadataResolverFactory {

  private final MetadataOutputResolver metadataOutputResolver;
  private final QueryEntityResolver queryMetadataEntityResolver;

  public QueryMetadataResolverFactory(Class<? extends MetadataOutputResolver> outputResolver,
                                      Class<? extends QueryEntityResolver> queryEntityResolver) {
    checkArgument(outputResolver != null, "MetadataOutputResolver type cannot be null");
    checkArgument(queryEntityResolver != null, "QueryEntityResolver type cannot be null");
    metadataOutputResolver = instantiateResolver(outputResolver);
    queryMetadataEntityResolver = instantiateResolver(queryEntityResolver);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataKeysResolver getKeyResolver() {
    return new NullMetadataResolver();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> MetadataContentResolver<T> getContentResolver() {
    return (MetadataContentResolver<T>) new NullMetadataResolver();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> MetadataAttributesResolver<T> getOutputAttributesResolver() {
    return (MetadataAttributesResolver<T>) new NullMetadataResolver();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> MetadataOutputResolver<T> getOutputResolver() {
    return new DsqlQueryMetadataResolver(queryMetadataEntityResolver, metadataOutputResolver);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public QueryEntityResolver getQueryEntityResolver() {
    return queryMetadataEntityResolver;
  }

  private <T> T instantiateResolver(Class<?> factoryType) {
    try {
      return (T) ClassUtils.instanciateClass(factoryType);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create MetadataResolver of type "
          + getClassName(factoryType)), e);
    }
  }
}
