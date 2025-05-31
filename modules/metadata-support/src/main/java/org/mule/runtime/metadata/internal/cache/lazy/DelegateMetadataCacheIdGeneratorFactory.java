/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metadata.internal.cache.lazy;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGenerator;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGeneratorFactory;
import org.mule.runtime.metadata.api.locator.ComponentLocator;

import java.util.function.Function;

import jakarta.inject.Inject;

public class DelegateMetadataCacheIdGeneratorFactory implements MetadataCacheIdGeneratorFactory<ComponentAst>, Initialisable {

  private final Function<Registry, MetadataCacheIdGeneratorFactory<ComponentAst>> metadataCacheIdGeneratorFactorySupplier;

  @Inject
  private Registry registry;

  private MetadataCacheIdGeneratorFactory<ComponentAst> metadataCacheIdGeneratorFactoryDelegate;

  public DelegateMetadataCacheIdGeneratorFactory(Function<Registry, MetadataCacheIdGeneratorFactory<ComponentAst>> metadataCacheIdGeneratorFactorySupplier) {
    this.metadataCacheIdGeneratorFactorySupplier = metadataCacheIdGeneratorFactorySupplier;
  }

  @Override
  public void initialise() throws InitialisationException {
    this.metadataCacheIdGeneratorFactoryDelegate = metadataCacheIdGeneratorFactorySupplier.apply(registry);
  }

  @Override
  public MetadataCacheIdGenerator<ComponentAst> create(DslResolvingContext context, ComponentLocator<ComponentAst> locator) {
    return metadataCacheIdGeneratorFactoryDelegate.create(context, locator);
  }

}
