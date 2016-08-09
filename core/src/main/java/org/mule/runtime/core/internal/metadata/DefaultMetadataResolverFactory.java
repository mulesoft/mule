/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.metadata;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.ClassUtils.getClassName;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.api.metadata.resolving.MetadataAttributesResolver;
import org.mule.runtime.api.metadata.resolving.MetadataContentResolver;
import org.mule.runtime.api.metadata.resolving.MetadataKeysResolver;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.extension.api.introspection.metadata.MetadataResolverFactory;


/**
 * Default implementation of the {@link MetadataResolverFactory}, it provides initialized instances of
 * {@link MetadataKeysResolver}, {@link MetadataKeysResolver} and {@link MetadataOutputResolver} of the classes passed in the
 * constructor.
 *
 * @since 4.0
 */
public final class DefaultMetadataResolverFactory implements MetadataResolverFactory {

  private final MetadataOutputResolver metadataOutputResolver;
  private final MetadataAttributesResolver metadataAttributesResolver;
  private final MetadataContentResolver metadataContentResolver;
  private final MetadataKeysResolver metadataKeysResolver;

  public DefaultMetadataResolverFactory(Class<? extends MetadataKeysResolver> keyResolver,
                                        Class<? extends MetadataContentResolver> contentResolver,
                                        Class<? extends MetadataOutputResolver> outputResolver,
                                        Class<? extends MetadataAttributesResolver> attributesResolver) {
    checkArgument(keyResolver != null, "MetadataKeyResolver type cannot be null");
    checkArgument(contentResolver != null, "MetadataContentResolver type cannot be null");
    checkArgument(outputResolver != null, "MetadataOutputResolver type cannot be null");

    metadataKeysResolver = instanciateResolver(keyResolver);
    metadataContentResolver = instanciateResolver(contentResolver);
    metadataOutputResolver = instanciateResolver(outputResolver);
    metadataAttributesResolver = instanciateResolver(attributesResolver);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataKeysResolver getKeyResolver() {
    return metadataKeysResolver;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> MetadataContentResolver<T> getContentResolver() {
    return metadataContentResolver;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> MetadataOutputResolver<T> getOutputResolver() {
    return metadataOutputResolver;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> MetadataAttributesResolver<T> getOutputAttributesResolver() {
    return metadataAttributesResolver;
  }

  private <T> T instanciateResolver(Class<?> factoryType) {
    try {
      return (T) ClassUtils.instanciateClass(factoryType);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create MetadataResolver of type "
          + getClassName(factoryType)), e);
    }
  }
}
