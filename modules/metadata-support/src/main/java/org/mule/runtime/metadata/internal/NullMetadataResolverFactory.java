/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metadata.internal;

import static java.util.Collections.emptyList;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.QueryEntityResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.metadata.NullQueryMetadataResolver;

import java.util.Collection;

/**
 * Null implementation of a {@link MetadataResolverFactory}, which returns a {@link NullMetadataResolver} for every resolver
 * provided by the factory
 *
 * @since 4.0
 */
public class NullMetadataResolverFactory implements MetadataResolverFactory {

  private final NullMetadataResolver metadataResolver;
  private final NullQueryMetadataResolver queryOutputMetadataResolver;

  public NullMetadataResolverFactory() {
    this.metadataResolver = new NullMetadataResolver();
    this.queryOutputMetadataResolver = new NullQueryMetadataResolver();
  }

  /**
   * @return a {@link NullMetadataResolver} implementation of {@link TypeKeysResolver}
   */
  @Override
  public TypeKeysResolver getKeyResolver() {
    return metadataResolver;
  }

  /**
   * @return a {@link NullMetadataResolver} implementation of {@link InputTypeResolver}
   */
  @Override
  public <T> InputTypeResolver<T> getInputResolver(String parameterName) {
    return (InputTypeResolver<T>) metadataResolver;
  }

  @Override
  public Collection<InputTypeResolver> getInputResolvers() {
    return emptyList();
  }

  /**
   * @return a {@link NullMetadataResolver} implementation of {@link OutputTypeResolver}
   */
  @Override
  public <T> OutputTypeResolver<T> getOutputResolver() {
    return (OutputTypeResolver<T>) metadataResolver;
  }

  /**
   * @return a {@link NullMetadataResolver} instance implementation of {@link AttributesTypeResolver}
   */
  @Override
  public <T> AttributesTypeResolver<T> getOutputAttributesResolver() {
    return (AttributesTypeResolver<T>) metadataResolver;
  }

  /**
   * @return a {@link NullQueryMetadataResolver} instance.
   */
  @Override
  public QueryEntityResolver getQueryEntityResolver() {
    return queryOutputMetadataResolver;
  }

}
