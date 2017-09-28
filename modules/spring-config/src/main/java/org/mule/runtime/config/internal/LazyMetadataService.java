/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.exception.ExceptionHelper.getRootException;
import static org.mule.runtime.api.metadata.resolving.FailureCode.COMPONENT_NOT_FOUND;
import static org.mule.runtime.api.metadata.resolving.MetadataFailure.Builder.newFailure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.config.internal.dsl.model.NoSuchComponentModelException;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@link MetadataService} implementation that initialises the required components before doing test connectivity.
 *
 * This guarantees that if the application has been created lazily, the requested components exists before the execution of the
 * actual {@link MetadataService}.
 *
 * @since 4.0
 */
public class LazyMetadataService implements MetadataService, Initialisable {

  public static final String NON_LAZY_METADATA_SERVICE = "_muleNonLazyMetadataService";

  private final LazyMuleArtifactContext lazyMuleArtifactContext;
  private final Supplier<MetadataService> metadataServiceSupplier;

  private MetadataService metadataService;

  public LazyMetadataService(LazyMuleArtifactContext lazyMuleArtifactContext, Supplier<MetadataService> metadataServiceSupplier) {
    this.lazyMuleArtifactContext = lazyMuleArtifactContext;
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

  private Optional<MetadataResult<?>> initializeComponent(Location location) {
    try {
      lazyMuleArtifactContext.initializeComponent(location);
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
    this.metadataService = metadataServiceSupplier.get();
  }
}
