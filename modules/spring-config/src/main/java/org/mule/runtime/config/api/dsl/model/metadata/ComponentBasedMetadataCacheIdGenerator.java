/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.core.internal.locator.ComponentLocator;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheId;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGenerator;

import java.util.Optional;

/**
 * A {@link ComponentModel} based implementation of a {@link MetadataCacheIdGenerator}
 *
 * @since 4.1.4, 4.2.0
 * @deprecated Use {@link ComponentAstBasedMetadataCacheIdGenerator} directly.
 */
@Deprecated
public class ComponentBasedMetadataCacheIdGenerator implements MetadataCacheIdGenerator<ComponentAst> {

  private final ComponentAstBasedMetadataCacheIdGenerator delegate;

  ComponentBasedMetadataCacheIdGenerator(DslResolvingContext context,
                                         ComponentLocator<ComponentAst> locator) {
    this.delegate = new ComponentAstBasedMetadataCacheIdGenerator(locator);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForComponentOutputMetadata(ComponentAst component) {
    checkArgument(component != null, "Cannot generate a Cache Key for a 'null' component");
    return delegate.getIdForComponentOutputMetadata(component);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForComponentAttributesMetadata(ComponentAst component) {
    checkArgument(component != null, "Cannot generate a Cache Key for a 'null' component");
    return delegate.getIdForComponentAttributesMetadata(component);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForComponentInputMetadata(ComponentAst component, String parameterName) {
    checkArgument(component != null, "Cannot generate a Cache Key for a 'null' component");
    return delegate.getIdForComponentInputMetadata(component, parameterName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForComponentMetadata(ComponentAst component) {
    checkArgument(component != null, "Cannot generate a Cache Key for a 'null' component");
    return delegate.getIdForComponentMetadata(component);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForMetadataKeys(ComponentAst component) {
    checkArgument(component != null, "Cannot generate a Cache Key for a 'null' component");
    return delegate.getIdForMetadataKeys(component);
  }

  @Override
  public Optional<MetadataCacheId> getIdForGlobalMetadata(ComponentAst component) {
    checkArgument(component != null, "Cannot generate a Cache Key for a 'null' component");
    return delegate.getIdForGlobalMetadata(component);
  }
}

