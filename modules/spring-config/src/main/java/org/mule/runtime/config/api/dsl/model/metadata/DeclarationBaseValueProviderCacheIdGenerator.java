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
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheIdGenerator;

import java.util.Optional;

public class DeclarationBaseValueProviderCacheIdGenerator implements ValueProviderCacheIdGenerator<ElementDeclaration> {

  private final DslElementModelFactory elementModelFactory;
  private final DslElementBasedValueProviderCacheIdGenerator delegate;

  public DeclarationBaseValueProviderCacheIdGenerator(DslResolvingContext context,
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
}
