/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.util.Optional.empty;
import static org.mule.runtime.api.metadata.resolving.FailureCode.COMPONENT_NOT_FOUND;
import static org.mule.runtime.api.metadata.resolving.MetadataFailure.Builder.newFailure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.metadata.ComponentId;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.config.spring.dsl.model.NoSuchComponentModelException;

import java.util.Optional;

/**
 * {@link MetadataService} implementation that initialises the required components before doing test
 * connectivity.
 *
 * This guarantees that if the application has been created lazily, the requested components exists
 * before the execution of the actual {@link MetadataService}.
 *
 * @since 4.0
 */
public class LazyMetadataService implements MetadataService {

  private final LazyMuleArtifactContext lazyMuleArtifactContext;
  private final MetadataService metadataService;

  public LazyMetadataService(LazyMuleArtifactContext lazyMuleArtifactContext, MetadataService metadataService) {

    this.lazyMuleArtifactContext = lazyMuleArtifactContext;
    this.metadataService = metadataService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<MetadataKeysContainer> getMetadataKeys(ComponentId componentId) {
    return (MetadataResult<MetadataKeysContainer>) initializeComponent(componentId)
        .orElseGet(() -> metadataService.getMetadataKeys(componentId));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<OperationModel>> getOperationMetadata(ComponentId componentId) {
    return (MetadataResult<ComponentMetadataDescriptor<OperationModel>>) initializeComponent(componentId)
        .orElseGet(() -> metadataService.getOperationMetadata(componentId));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<OperationModel>> getOperationMetadata(ComponentId componentId,
                                                                                          MetadataKey key) {
    return (MetadataResult<ComponentMetadataDescriptor<OperationModel>>) initializeComponent(componentId)
        .orElseGet(() -> metadataService.getOperationMetadata(componentId, key));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<SourceModel>> getSourceMetadata(ComponentId componentId) {
    return (MetadataResult<ComponentMetadataDescriptor<SourceModel>>) initializeComponent(componentId)
        .orElseGet(() -> metadataService.getSourceMetadata(componentId));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<ComponentMetadataDescriptor<SourceModel>> getSourceMetadata(ComponentId componentId, MetadataKey key) {
    return (MetadataResult<ComponentMetadataDescriptor<SourceModel>>) initializeComponent(componentId)
        .orElseGet(() -> metadataService.getSourceMetadata(componentId, key));
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
  public MetadataResult<MetadataKeysContainer> getEntityKeys(ComponentId componentId) {
    return (MetadataResult<MetadataKeysContainer>) initializeComponent(componentId)
        .orElseGet(() -> metadataService.getEntityKeys(componentId));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MetadataResult<TypeMetadataDescriptor> getEntityMetadata(ComponentId componentId, MetadataKey key) {
    return (MetadataResult<TypeMetadataDescriptor>) initializeComponent(componentId)
        .orElseGet(() -> metadataService.getEntityMetadata(componentId, key));
  }

  private Optional<MetadataResult<?>> initializeComponent(ComponentId componentId) {
    //TODO MULE-9496: REFACTOR WHEN FLOW PATH IS AVAILABLE
    final String componentIdentifier = componentId.getFlowName().isPresent()
        ? componentId.getFlowName().get() + "/" + componentId.getComponentPath() : componentId.getComponentPath();
    try {
      lazyMuleArtifactContext.initializeComponent(componentIdentifier);
    } catch (NoSuchComponentModelException e) {
      return Optional.of(failure(newFailure(e).withFailureCode(COMPONENT_NOT_FOUND).onComponent()));
    } catch (Exception e) {
      return Optional.of(failure(newFailure(e).onComponent()));
    }
    return empty();
  }
}
