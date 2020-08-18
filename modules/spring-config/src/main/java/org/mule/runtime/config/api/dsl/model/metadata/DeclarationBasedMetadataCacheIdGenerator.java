/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.core.internal.locator.ComponentLocator;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheId;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGenerator;

import java.util.Optional;

public class DeclarationBasedMetadataCacheIdGenerator implements MetadataCacheIdGenerator<ElementDeclaration> {

  private final DslElementModelFactory elementModelFactory;
  private final DslElementBasedMetadataCacheIdGenerator delegate;

  public DeclarationBasedMetadataCacheIdGenerator(DslResolvingContext context,
                                                  ComponentLocator<ElementDeclaration> locator) {
    this.elementModelFactory = DslElementModelFactory.getDefault(context);
    this.delegate = new DslElementBasedMetadataCacheIdGenerator(
                                                                l -> locator.get(l)
                                                                    .map(e -> elementModelFactory.create(e).orElse(null)));
  }

  @Override
  public Optional<MetadataCacheId> getIdForComponentOutputMetadata(ElementDeclaration component) {
    checkArgument(component != null, "Cannot generate a Cache Key for a 'null' component");
    return elementModelFactory.create(component).flatMap(delegate::getIdForComponentOutputMetadata);
  }

  @Override
  public Optional<MetadataCacheId> getIdForComponentAttributesMetadata(ElementDeclaration component) {
    checkArgument(component != null, "Cannot generate a Cache Key for a 'null' component");
    return elementModelFactory.create(component).flatMap(delegate::getIdForComponentAttributesMetadata);
  }

  @Override
  public Optional<MetadataCacheId> getIdForComponentInputMetadata(ElementDeclaration component, String parameterName) {
    checkArgument(component != null, "Cannot generate a Cache Key for a 'null' component");
    return elementModelFactory.create(component).flatMap(e -> delegate.getIdForComponentInputMetadata(e, parameterName));
  }

  @Override
  public Optional<MetadataCacheId> getIdForComponentMetadata(ElementDeclaration component) {
    checkArgument(component != null, "Cannot generate a Cache Key for a 'null' component");
    return elementModelFactory.create(component).flatMap(delegate::getIdForComponentMetadata);
  }

  @Override
  public Optional<MetadataCacheId> getIdForMetadataKeys(ElementDeclaration component) {
    checkArgument(component != null, "Cannot generate a Cache Key for a 'null' component");
    return elementModelFactory.create(component).flatMap(delegate::getIdForMetadataKeys);
  }

  @Override
  public Optional<MetadataCacheId> getIdForGlobalMetadata(ElementDeclaration component) {
    checkArgument(component != null, "Cannot generate a Cache Key for a 'null' component");
    return elementModelFactory.create(component).flatMap(delegate::getIdForGlobalMetadata);
  }
}
