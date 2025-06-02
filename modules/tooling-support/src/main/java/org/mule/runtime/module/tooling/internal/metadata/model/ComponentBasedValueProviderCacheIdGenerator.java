/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.metadata.model;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.metadata.api.locator.ComponentLocator;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheIdGenerator;
import org.mule.runtime.metadata.internal.cache.DslElementBasedValueProviderCacheIdGenerator;

import java.util.Optional;

public class ComponentBasedValueProviderCacheIdGenerator implements ValueProviderCacheIdGenerator<ComponentAst> {

  private final DslElementModelFactory elementModelFactory;
  private final DslElementBasedValueProviderCacheIdGenerator delegate;

  public ComponentBasedValueProviderCacheIdGenerator(DslResolvingContext context,
                                                     ComponentLocator<ComponentAst> locator) {
    this.elementModelFactory = DslElementModelFactory.getDefault(context);
    this.delegate = new DslElementBasedValueProviderCacheIdGenerator(location -> locator.get(location)
        .map(c -> elementModelFactory.create(c)
            .orElse(null)));
  }

  @Override
  public Optional<ValueProviderCacheId> getIdForResolvedValues(ComponentAst containerComponent, String parameterName) {
    checkArgument(containerComponent != null, "Cannot generate a Cache Key for a 'null' component");
    return elementModelFactory.create(containerComponent).flatMap(e -> delegate.getIdForResolvedValues(e, parameterName));
  }

  @Override
  public Optional<ValueProviderCacheId> getIdForResolvedValues(ComponentAst containerComponent, String parameterName,
                                                               String targetPath) {
    checkArgument(containerComponent != null, "Cannot generate a Cache Key for a 'null' component");
    return elementModelFactory.create(containerComponent)
        .flatMap(e -> delegate.getIdForResolvedValues(e, parameterName, targetPath));
  }
}
