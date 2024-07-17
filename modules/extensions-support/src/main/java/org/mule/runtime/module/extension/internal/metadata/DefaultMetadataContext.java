/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.message.api.el.TypeBindings;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.api.metadata.ScopeOutputMetadataContext;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.RouterOutputMetadataContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.privileged.metadata.InternalMetadataContext;
import org.mule.runtime.module.extension.internal.ExtensionResolvingContext;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Default immutable implementation of {@link MetadataContext}, it provides access to the extension configuration and connection
 * in the metadata fetch invocation.
 *
 * @since 4.0
 */
public class DefaultMetadataContext extends ExtensionResolvingContext implements ConnectionProviderAwareMetadataContext,
    InternalMetadataContext {

  private final MetadataCache cache;
  private final ClassTypeLoader classTypeLoader;
  private final Optional<ScopeOutputMetadataContext> scopePropagationContext;
  private final Optional<RouterOutputMetadataContext> routerPropagationContext;
  private final Optional<ExpressionLanguageMetadataService> expressionLanguageMetadataService;
  private final Optional<TypeBindings> typeBindings;

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
    this(configurationSupplier, connectionManager, cache, typeLoader, empty(), empty());
  }

  /**
   * Retrieves the configuration for the related component
   *
   * @param configurationSupplier    Supplier of optional configurations
   * @param connectionManager        {@link ConnectionManager} which is able to find a connection for the component using the
   *                                 {@param configInstance}
   * @param cache                    instance of the {@link MetadataCache} for this context
   * @param typeLoader               instance of a {@link ClassTypeLoader} in the context of this extension
   * @param scopePropagationContext  an optional {@link ScopeOutputMetadataContext} for the inner chain. Value is only present for
   *                                 scope components
   * @param routerPropagationContext an optional {@link RouterOutputMetadataContext} with routes information. Value is only
   *                                 present for router components
   */
  public DefaultMetadataContext(Supplier<Optional<ConfigurationInstance>> configurationSupplier,
                                ConnectionManager connectionManager, MetadataCache cache, ClassTypeLoader typeLoader,
                                Optional<ScopeOutputMetadataContext> scopePropagationContext,
                                Optional<RouterOutputMetadataContext> routerPropagationContext) {
    this(configurationSupplier, connectionManager, cache, typeLoader, null, scopePropagationContext, routerPropagationContext,
         empty());
  }

  /**
   * Retrieves the configuration for the related component
   *
   * @param configurationSupplier    Supplier of optional configurations
   * @param connectionManager        {@link ConnectionManager} which is able to find a connection for the component using the
   *                                 {@param configInstance}
   * @param cache                    instance of the {@link MetadataCache} for this context
   * @param typeLoader               instance of a {@link ClassTypeLoader} in the context of this extension
   * @param scopePropagationContext  an optional {@link ScopeOutputMetadataContext} for the inner chain. Value is only present for
   *                                 scope components
   * @param routerPropagationContext an optional {@link RouterOutputMetadataContext} with routes information. Value is only
   *                                 present for router components
   */
  public DefaultMetadataContext(Supplier<Optional<ConfigurationInstance>> configurationSupplier,
                                ConnectionManager connectionManager, MetadataCache cache, ClassTypeLoader typeLoader,
                                ExpressionLanguageMetadataService expressionLanguageMetadataService,
                                Optional<ScopeOutputMetadataContext> scopePropagationContext,
                                Optional<RouterOutputMetadataContext> routerPropagationContext,
                                Optional<TypeBindings> typeBindings) {
    super(configurationSupplier, connectionManager);
    this.cache = cache;
    this.classTypeLoader = typeLoader;
    this.expressionLanguageMetadataService = ofNullable(expressionLanguageMetadataService);
    this.scopePropagationContext = scopePropagationContext;
    this.routerPropagationContext = routerPropagationContext;
    this.typeBindings = typeBindings;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataCache getCache() {
    return cache;
  }

  @Override
  public Optional<ScopeOutputMetadataContext> getScopeOutputMetadataContext() {
    return scopePropagationContext;
  }

  @Override
  public Optional<RouterOutputMetadataContext> getRouterOutputMetadataContext() {
    return routerPropagationContext;
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

  @Override
  public Optional<ExpressionLanguageMetadataService> getExpressionLanguageMetadataService() {
    return expressionLanguageMetadataService;
  }

  @Override
  public Optional<TypeBindings> getTypeBindings() {
    return typeBindings;
  }
}
