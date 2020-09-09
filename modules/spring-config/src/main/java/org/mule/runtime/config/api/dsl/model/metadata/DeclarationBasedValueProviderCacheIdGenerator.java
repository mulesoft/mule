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

  //This method was added so that it can be called by reflection and we can keep the logic on how
  //the ValueProviderCacheId is generated for configs and connections in one place, without changing the
  //ValueProviderCacheIdGenerator API.
  //I could not come with a better solution that does not involve refactoring of the whole API. Having
  //said that, we have encountered multiple scenarios where we are needing more information than the
  //currently provided by the ValueProviderCacheId (Hierarchical info for example). Meaning that we should
  //consider refactoring this in the future to make things simpler. MULE-18743
  private Optional<ValueProviderCacheId> getIdForDependency(ElementDeclaration elementDeclaration) {
    return elementModelFactory.create(elementDeclaration).flatMap(delegate::resolveIdForInjectedElement);
  }


}
