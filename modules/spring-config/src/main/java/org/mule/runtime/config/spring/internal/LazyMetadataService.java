/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal;

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
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.config.spring.internal.dsl.model.NoSuchComponentModelException;

import java.util.function.Supplier;
import java.util.concurrent.Callable;

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

  private final LazyComponentTaskExecutor lazyComponentTaskExecutor;
  private final Supplier<MetadataService> metadataServiceSupplier;

  private MetadataService metadataService;

  public LazyMetadataService(LazyComponentTaskExecutor lazyComponentTaskExecutor,
                             Supplier<MetadataService> metadataServiceSupplier) {
    this.lazyComponentTaskExecutor = lazyComponentTaskExecutor;
    this.metadataServiceSupplier = metadataServiceSupplier;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<MetadataKeysContainer> getMetadataKeys(Location location) {
    return (MetadataResult<MetadataKeysContainer>) resolveMetadata(location, () -> metadataService.getMetadataKeys(location));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<OperationModel>> getOperationMetadata(Location location) {
    return (MetadataResult<ComponentMetadataDescriptor<OperationModel>>) resolveMetadata(location, () -> metadataService
        .getOperationMetadata(location));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<OperationModel>> getOperationMetadata(Location location,
                                                                                          MetadataKey key) {
    return (MetadataResult<ComponentMetadataDescriptor<OperationModel>>) resolveMetadata(location, () -> metadataService
        .getOperationMetadata(location, key));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<SourceModel>> getSourceMetadata(Location location) {
    return (MetadataResult<ComponentMetadataDescriptor<SourceModel>>) resolveMetadata(location, () -> metadataService
        .getSourceMetadata(location));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<SourceModel>> getSourceMetadata(Location location, MetadataKey key) {
    return (MetadataResult<ComponentMetadataDescriptor<SourceModel>>) resolveMetadata(location, () -> metadataService
        .getSourceMetadata(location, key));
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
    return (MetadataResult<MetadataKeysContainer>) resolveMetadata(location, () -> metadataService.getEntityKeys(location));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<TypeMetadataDescriptor> getEntityMetadata(Location location, MetadataKey key) {
    return (MetadataResult<TypeMetadataDescriptor>) resolveMetadata(location,
                                                                    () -> metadataService.getEntityMetadata(location, key));
  }

  private MetadataResult<?> resolveMetadata(Location location, Callable<MetadataResult<?>> metadataResultCallable) {
    try {
      return lazyComponentTaskExecutor.withContext(location, () -> metadataResultCallable.call());
    } catch (Exception e) {
      if (getRootException(e) instanceof NoSuchComponentModelException) {
        return failure(newFailure(e).withFailureCode(COMPONENT_NOT_FOUND).onComponent());
      }
      return failure(newFailure(e).onComponent());
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    this.metadataService = metadataServiceSupplier.get();
  }
}
