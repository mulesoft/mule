/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.api.dsl.model.metadata;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.metadata.api.locator.ComponentLocator;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGenerator;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGeneratorFactory;


/**
 * A {@link ComponentAst} based implementation of a {@link MetadataCacheIdGeneratorFactory}
 *
 * @since 4.1.4, 4.2.0
 */
public class ModelBasedMetadataCacheIdGeneratorFactory implements MetadataCacheIdGeneratorFactory<ComponentAst> {

  @Override
  public MetadataCacheIdGenerator<ComponentAst> create(DslResolvingContext context,
                                                       ComponentLocator<ComponentAst> locator) {
    return new ComponentBasedMetadataCacheIdGenerator(context, locator);
  }
}
