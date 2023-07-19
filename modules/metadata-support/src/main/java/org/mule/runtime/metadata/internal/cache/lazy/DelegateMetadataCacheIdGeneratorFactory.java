/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metadata.internal.cache.lazy;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGenerator;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGeneratorFactory;
import org.mule.runtime.metadata.api.locator.ComponentLocator;

import java.util.function.Supplier;

public class DelegateMetadataCacheIdGeneratorFactory implements MetadataCacheIdGeneratorFactory<ComponentAst>, Initialisable {

  private final Supplier<MetadataCacheIdGeneratorFactory<ComponentAst>> metadataCacheIdGeneratorFactorySupplier;
  private MetadataCacheIdGeneratorFactory<ComponentAst> metadataCacheIdGeneratorFactoryDelegate;

  public DelegateMetadataCacheIdGeneratorFactory(Supplier<MetadataCacheIdGeneratorFactory<ComponentAst>> metadataCacheIdGeneratorFactorySupplier) {
    this.metadataCacheIdGeneratorFactorySupplier = metadataCacheIdGeneratorFactorySupplier;
  }

  @Override
  public void initialise() throws InitialisationException {
    this.metadataCacheIdGeneratorFactoryDelegate = metadataCacheIdGeneratorFactorySupplier.get();
  }

  @Override
  public MetadataCacheIdGenerator<ComponentAst> create(DslResolvingContext context, ComponentLocator<ComponentAst> locator) {
    return metadataCacheIdGeneratorFactoryDelegate.create(context, locator);
  }

}
