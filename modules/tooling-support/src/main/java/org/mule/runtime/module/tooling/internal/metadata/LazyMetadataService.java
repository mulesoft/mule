/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.metadata;

import static org.mule.runtime.api.exception.ExceptionHelper.getRootException;
import static org.mule.runtime.api.metadata.resolving.FailureCode.COMPONENT_NOT_FOUND;
import static org.mule.runtime.api.metadata.resolving.MetadataFailure.Builder.newFailure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.RouterOutputMetadataContext;
import org.mule.runtime.api.metadata.ScopeOutputMetadataContext;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.InputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.OutputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.RouterInputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.ScopeInputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.config.internal.context.lazy.LazyComponentInitializerAdapter;
import org.mule.runtime.config.internal.context.lazy.NoSuchComponentModelException;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.inject.Inject;

/**
 * {@link MetadataService} implementation that initialises the required components before doing test connectivity.
 *
 * This guarantees that if the application has been created lazily, the requested components exists before the execution of the
 * actual {@link MetadataService}.
 *
 * @since 4.0
 */
public class LazyMetadataService implements MetadataService, Initialisable {

  private final Function<Registry, MetadataService> metadataServiceSupplier;

  @Inject
  private LazyComponentInitializerAdapter lazyMuleArtifactContext;

  @Inject
  private Registry registry;

  private MetadataService metadataService;

  public LazyMetadataService(Function<Registry, MetadataService> metadataServiceSupplier) {
    this.metadataServiceSupplier = metadataServiceSupplier;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<MetadataKeysContainer> getMetadataKeys(Location location) {
    return (MetadataResult<MetadataKeysContainer>) initializeComponent(location)
        .orElseGet(() -> metadataService.getMetadataKeys(location));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<MetadataKeysContainer> getMetadataKeys(Location location, MetadataKey partialKey) {
    return (MetadataResult<MetadataKeysContainer>) initializeComponent(location)
        .orElseGet(() -> metadataService.getMetadataKeys(location, partialKey));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<InputMetadataDescriptor> getInputMetadata(Location location, MetadataKey key) {
    return (MetadataResult<InputMetadataDescriptor>) initializeComponent(location)
        .orElseGet(() -> metadataService.getInputMetadata(location, key));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<OutputMetadataDescriptor> getOutputMetadata(Location location, MetadataKey key) {
    return (MetadataResult<OutputMetadataDescriptor>) initializeComponent(location)
        .orElseGet(() -> metadataService.getOutputMetadata(location, key));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<OperationModel>> getOperationMetadata(Location location) {
    return (MetadataResult<ComponentMetadataDescriptor<OperationModel>>) initializeComponent(location)
        .orElseGet(() -> metadataService.getOperationMetadata(location));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<OperationModel>> getOperationMetadata(Location location,
                                                                                          MetadataKey key) {
    return (MetadataResult<ComponentMetadataDescriptor<OperationModel>>) initializeComponent(location)
        .orElseGet(() -> metadataService.getOperationMetadata(location, key));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<SourceModel>> getSourceMetadata(Location location) {
    return (MetadataResult<ComponentMetadataDescriptor<SourceModel>>) initializeComponent(location)
        .orElseGet(() -> metadataService.getSourceMetadata(location));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<SourceModel>> getSourceMetadata(Location location, MetadataKey key) {
    return (MetadataResult<ComponentMetadataDescriptor<SourceModel>>) initializeComponent(location)
        .orElseGet(() -> metadataService.getSourceMetadata(location, key));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void disposeCache(String id) {
    metadataService.disposeCache(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<MetadataKeysContainer> getEntityKeys(Location location) {
    return (MetadataResult<MetadataKeysContainer>) initializeComponent(location)
        .orElseGet(() -> metadataService.getEntityKeys(location));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<TypeMetadataDescriptor> getEntityMetadata(Location location, MetadataKey key) {
    return (MetadataResult<TypeMetadataDescriptor>) initializeComponent(location)
        .orElseGet(() -> metadataService.getEntityMetadata(location, key));
  }

  @Override
  public MetadataResult<ScopeInputMetadataDescriptor> getScopeInputMetadata(Location location,
                                                                            MetadataKey key,
                                                                            Supplier<MessageMetadataType> scopeInputMessageType) {
    return (MetadataResult<ScopeInputMetadataDescriptor>) initializeComponent(location)
        .orElseGet(() -> metadataService.getScopeInputMetadata(location, key, scopeInputMessageType));
  }

  @Override
  public MetadataResult<RouterInputMetadataDescriptor> getRouterInputMetadata(Location location,
                                                                              MetadataKey key,
                                                                              Supplier<MessageMetadataType> routerInputMessageType) {
    return (MetadataResult<RouterInputMetadataDescriptor>) initializeComponent(location)
        .orElseGet(() -> metadataService.getRouterInputMetadata(location, key, routerInputMessageType));
  }

  @Override
  public MetadataResult<OutputMetadataDescriptor> getScopeOutputMetadata(Location location, MetadataKey key,
                                                                         ScopeOutputMetadataContext ctx) {
    return (MetadataResult<OutputMetadataDescriptor>) initializeComponent(location)
        .orElseGet(() -> metadataService.getScopeOutputMetadata(location, key, ctx));
  }

  @Override
  public MetadataResult<OutputMetadataDescriptor> getRouterOutputMetadata(Location location, MetadataKey key,
                                                                          RouterOutputMetadataContext ctx) {
    return (MetadataResult<OutputMetadataDescriptor>) initializeComponent(location)
        .orElseGet(() -> metadataService.getRouterOutputMetadata(location, key, ctx));
  }

  private Optional<MetadataResult<?>> initializeComponent(Location location) {
    try {
      lazyMuleArtifactContext.initializeComponent(location, false);
    } catch (Exception e) {
      Throwable rootCause = getRootException(e);
      MetadataFailure.Builder builder = newFailure(e).withMessage(rootCause.getMessage());
      if (rootCause instanceof NoSuchComponentModelException) {
        builder.withFailureCode(COMPONENT_NOT_FOUND);
      }
      return of(failure(builder.onComponent()));
    }
    return empty();
  }

  @Override
  public void initialise() throws InitialisationException {
    this.metadataService = metadataServiceSupplier.apply(registry);
  }
}
