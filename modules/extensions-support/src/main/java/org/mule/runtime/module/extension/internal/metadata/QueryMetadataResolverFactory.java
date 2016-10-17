/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.runtime.core.config.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.ClassUtils.getClassName;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.QueryEntityResolver;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.extension.api.annotation.Query;
import org.mule.runtime.extension.api.introspection.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.introspection.metadata.NullMetadataResolver;


/**
 * A {@link MetadataResolverFactory} implementation for {@link Query} operations, it provides initialized instances of
 * {@link OutputTypeResolver} and {@link QueryEntityResolver}.
 *
 * {@link AttributesTypeResolver}, {@link InputTypeResolver} and {@link TypeKeysResolver} returned instances are
 * always instances of {@link NullMetadataResolver}.
 *
 * @since 4.0
 */
public final class QueryMetadataResolverFactory implements MetadataResolverFactory {

  private final OutputTypeResolver outputTypeResolver;
  private final QueryEntityResolver queryMetadataEntityResolver;

  public QueryMetadataResolverFactory(Class<? extends OutputTypeResolver> outputResolver,
                                      Class<? extends QueryEntityResolver> queryEntityResolver) {
    checkArgument(outputResolver != null, "OutputTypeResolver type cannot be null");
    checkArgument(queryEntityResolver != null, "QueryEntityResolver type cannot be null");
    outputTypeResolver = instantiateResolver(outputResolver);
    queryMetadataEntityResolver = instantiateResolver(queryEntityResolver);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TypeKeysResolver getKeyResolver() {
    return new NullMetadataResolver();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> InputTypeResolver<T> getInputResolver(String parameterName) {
    return (InputTypeResolver<T>) new NullMetadataResolver();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> AttributesTypeResolver<T> getOutputAttributesResolver() {
    return (AttributesTypeResolver<T>) new NullMetadataResolver();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> OutputTypeResolver<T> getOutputResolver() {
    return new DsqlQueryMetadataResolver(queryMetadataEntityResolver, outputTypeResolver);
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
      throw new MuleRuntimeException(createStaticMessage("Could not create NamedTypeResolver of type "
          + getClassName(factoryType)), e);
    }
  }
}
