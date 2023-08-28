/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.api.dsl.model.metadata;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.metadata.api.locator.ComponentLocator;
import org.mule.runtime.metadata.api.cache.MetadataCacheId;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGenerator;
import org.mule.runtime.metadata.internal.cache.DslElementBasedMetadataCacheIdGenerator;

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
