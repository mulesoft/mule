/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.module.extension.internal.ExtensionResolvingContext;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Default immutable implementation of {@link MetadataContext}, it provides access to the extension configuration and
 * connection in the metadata fetch invocation.
 *
 * @since 4.0
 */
public class DefaultMetadataContext extends ExtensionResolvingContext implements MetadataContext {

  private final MetadataCache cache;
  private final ClassTypeLoader classTypeLoader;

  /**
   * Retrieves the configuration for the related component
   *
   * @param configurationSupplier Supplier of optional configurations
   * @param connectionManager     {@link ConnectionManager} which is able to find a connection for the component using
   *                              the {@param configInstance}
   * @param cache                 instance of the {@link MetadataCache} for this context
   * @param typeLoader            instance of a {@link ClassTypeLoader} in the context of this extension
   */
  public DefaultMetadataContext(Supplier<Optional<ConfigurationInstance>> configurationSupplier,
                                ConnectionManager connectionManager, MetadataCache cache, ClassTypeLoader typeLoader) {
    super(configurationSupplier, connectionManager);
    this.cache = cache;
    this.classTypeLoader = typeLoader;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataCache getCache() {
    return cache;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ClassTypeLoader getTypeLoader() {
    return classTypeLoader;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BaseTypeBuilder getTypeBuilder() {
    return BaseTypeBuilder.create(JAVA);
  }
}
