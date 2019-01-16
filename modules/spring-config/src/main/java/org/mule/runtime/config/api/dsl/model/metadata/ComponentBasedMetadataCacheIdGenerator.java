/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheId;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGenerator;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGeneratorFactory.ComponentLocator;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;

import java.util.Optional;

/**
 * A {@link ComponentModel} based implementation of a {@link MetadataCacheIdGenerator}
 *
 * @since 4.1.4, 4.2.0
 */
public class ComponentBasedMetadataCacheIdGenerator implements MetadataCacheIdGenerator<ComponentConfiguration> {

  private final DslElementModelFactory elementModelFactory;
  private final DslElementBasedMetadataCacheIdGenerator delegate;

  ComponentBasedMetadataCacheIdGenerator(DslResolvingContext context,
                                         ComponentLocator<ComponentConfiguration> locator) {
    this.elementModelFactory = DslElementModelFactory.getDefault(context);
    this.delegate = new DslElementBasedMetadataCacheIdGenerator(location -> locator.get(location)
        .map(c -> elementModelFactory.create(c)
            .orElse(null)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForComponentOutputMetadata(ComponentConfiguration component) {
    checkArgument(component != null, "Cannot generate a Cache Key for a 'null' component");
    return elementModelFactory.create(component)
        .map(e -> delegate.getIdForComponentOutputMetadata(e).orElse(null));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForComponentAttributesMetadata(ComponentConfiguration component) {
    checkArgument(component != null, "Cannot generate a Cache Key for a 'null' component");
    return elementModelFactory.create(component)
        .map(e -> delegate.getIdForComponentAttributesMetadata(e).orElse(null));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForComponentInputMetadata(ComponentConfiguration component, String parameterName) {
    checkArgument(component != null, "Cannot generate a Cache Key for a 'null' component");
    return elementModelFactory.create(component)
        .map(e -> delegate.getIdForComponentInputMetadata(e, parameterName).orElse(null));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForComponentMetadata(ComponentConfiguration component) {
    checkArgument(component != null, "Cannot generate a Cache Key for a 'null' component");
    return elementModelFactory.create(component)
        .map(e -> delegate.getIdForComponentMetadata(e).orElse(null));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForMetadataKeys(ComponentConfiguration component) {
    checkArgument(component != null, "Cannot generate a Cache Key for a 'null' component");
    return elementModelFactory.create(component)
        .map(e -> delegate.getIdForMetadataKeys(e).orElse(null));
  }

  @Override
  public Optional<MetadataCacheId> getIdForGlobalMetadata(ComponentConfiguration component) {
    checkArgument(component != null, "Cannot generate a Cache Key for a 'null' component");
    return elementModelFactory.create(component)
        .map(e -> delegate.getIdForGlobalMetadata(e).orElse(null));
  }
}

