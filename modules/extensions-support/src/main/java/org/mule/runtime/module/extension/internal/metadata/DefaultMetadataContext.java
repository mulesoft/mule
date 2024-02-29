/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.module.extension.internal.ExtensionResolvingContext;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Default immutable implementation of {@link MetadataContext}, it provides access to the extension configuration and connection
 * in the metadata fetch invocation.
 *
 * @since 4.0
 */
public class DefaultMetadataContext extends ExtensionResolvingContext implements ConnectionProviderAwareMetadataContext {

  private final MetadataCache cache;
  private final ClassTypeLoader classTypeLoader;
  private final Optional<Supplier<MetadataType>> innerChainOutputType;
  private final Map<String, Supplier<MetadataType>> innerRoutesOutputType;

  /**
   * Retrieves the configuration for the related component
   *
   * @param configurationSupplier Supplier of optional configurations
   * @param connectionManager     {@link ConnectionManager} which is able to find a connection for the component using the
   *                              {@param configInstance}
   * @param cache                 instance of the {@link MetadataCache} for this context
   * @param typeLoader            instance of a {@link ClassTypeLoader} in the context of this extension
   */
  public DefaultMetadataContext(Supplier<Optional<ConfigurationInstance>> configurationSupplier,
                                ConnectionManager connectionManager, MetadataCache cache, ClassTypeLoader typeLoader) {
    this(configurationSupplier, connectionManager, cache, typeLoader, empty(), emptyMap());
  }

  /**
   * Retrieves the configuration for the related component
   *
   * @param configurationSupplier Supplier of optional configurations
   * @param connectionManager     {@link ConnectionManager} which is able to find a connection for the component using the
   *                              {@param configInstance}
   * @param cache                 instance of the {@link MetadataCache} for this context
   * @param typeLoader            instance of a {@link ClassTypeLoader} in the context of this extension
   * @param innerChainOutputType  Supplier of the {@link MetadataType} of the inner chain of a scope
   */
  public DefaultMetadataContext(Supplier<Optional<ConfigurationInstance>> configurationSupplier,
                                ConnectionManager connectionManager, MetadataCache cache, ClassTypeLoader typeLoader,
                                Supplier<MetadataType> innerChainOutputType) {
    this(configurationSupplier, connectionManager, cache, typeLoader, of(innerChainOutputType), emptyMap());
  }

  /**
   * Retrieves the configuration for the related component
   *
   * @param configurationSupplier Supplier of optional configurations
   * @param connectionManager     {@link ConnectionManager} which is able to find a connection for the component using the
   *                              {@param configInstance}
   * @param cache                 instance of the {@link MetadataCache} for this context
   * @param typeLoader            instance of a {@link ClassTypeLoader} in the context of this extension
   * @param innerRoutesOutputType a map of suppliers for the {@link MetadataType} corresping to each router's routes (location)
   */
  public DefaultMetadataContext(Supplier<Optional<ConfigurationInstance>> configurationSupplier,
                                ConnectionManager connectionManager, MetadataCache cache, ClassTypeLoader typeLoader,
                                Map<String, Supplier<MetadataType>> innerRoutesOutputType) {
    this(configurationSupplier, connectionManager, cache, typeLoader, empty(), innerRoutesOutputType);
  }

  private DefaultMetadataContext(Supplier<Optional<ConfigurationInstance>> configurationSupplier,
                                 ConnectionManager connectionManager, MetadataCache cache, ClassTypeLoader typeLoader,
                                 Optional<Supplier<MetadataType>> innerChainOutputType,
                                 Map<String, Supplier<MetadataType>> innerRoutesOutputType) {
    super(configurationSupplier, connectionManager);
    this.cache = cache;
    this.classTypeLoader = typeLoader;
    this.innerRoutesOutputType = innerRoutesOutputType;
    this.innerChainOutputType = innerChainOutputType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataCache getCache() {
    return cache;
  }

  @Override
  public Optional<Supplier<MetadataType>> getInnerChainOutputType() {
    return innerChainOutputType;
  }

  @Override
  public Map<String, Supplier<MetadataType>> getInnerRoutesOutputType() {
    return innerRoutesOutputType;
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
