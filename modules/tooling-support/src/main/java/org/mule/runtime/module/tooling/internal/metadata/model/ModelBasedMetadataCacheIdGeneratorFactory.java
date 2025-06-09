/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.metadata.model;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGenerator;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGeneratorFactory;
import org.mule.runtime.metadata.api.locator.ComponentLocator;
import org.mule.runtime.metadata.internal.cache.ComponentAstBasedMetadataCacheIdGenerator;


/**
 * A {@link ComponentAst} based implementation of a {@link MetadataCacheIdGeneratorFactory}
 *
 * @since 4.1.4, 4.2.0
 */
public class ModelBasedMetadataCacheIdGeneratorFactory implements MetadataCacheIdGeneratorFactory<ComponentAst> {

  @Override
  public MetadataCacheIdGenerator<ComponentAst> create(DslResolvingContext context,
                                                       ComponentLocator<ComponentAst> locator) {
    return new ComponentAstBasedMetadataCacheIdGenerator(locator);
  }
}
