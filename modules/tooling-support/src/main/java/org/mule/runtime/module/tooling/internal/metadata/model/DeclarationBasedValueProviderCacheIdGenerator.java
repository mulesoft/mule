/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.metadata.model;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.metadata.api.locator.ComponentLocator;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheIdGenerator;
import org.mule.runtime.metadata.internal.cache.DslElementBasedValueProviderCacheIdGenerator;

import java.util.Optional;

@NoExtend
public class DeclarationBasedValueProviderCacheIdGenerator implements ValueProviderCacheIdGenerator<ElementDeclaration> {

  private final DslElementModelFactory elementModelFactory;
  private final DslElementBasedValueProviderCacheIdGenerator delegate;

  public DeclarationBasedValueProviderCacheIdGenerator(DslResolvingContext context,
                                                       ComponentLocator<ElementDeclaration> locator) {
    this.elementModelFactory = DslElementModelFactory.getDefault(context);
    this.delegate = new DslElementBasedValueProviderCacheIdGenerator(
                                                                     l -> locator.get(l)
                                                                         .map(e -> elementModelFactory.create(e).orElse(null)));
  }

  @Override
  public Optional<ValueProviderCacheId> getIdForResolvedValues(ElementDeclaration containerComponent, String parameterName) {
    checkArgument(containerComponent != null, "Cannot generate a Cache Key for a 'null' component");
    return elementModelFactory.create(containerComponent).flatMap(dsl -> delegate.getIdForResolvedValues(dsl, parameterName));
  }

  @Override
  public Optional<ValueProviderCacheId> getIdForResolvedValues(ElementDeclaration containerComponent, String parameterName,
                                                               String targetPath) {
    checkArgument(containerComponent != null, "Cannot generate a Cache Key for a 'null' component");
    return elementModelFactory.create(containerComponent)
        .flatMap(dsl -> delegate.getIdForResolvedValues(dsl, parameterName, targetPath));
  }

  // Consider refactoring this in the future to make things simpler. MULE-18743
  protected Optional<ValueProviderCacheId> getIdForDependency(ElementDeclaration elementDeclaration) {
    return elementModelFactory.create(elementDeclaration).flatMap(delegate::resolveIdForInjectedElement);
  }

}
