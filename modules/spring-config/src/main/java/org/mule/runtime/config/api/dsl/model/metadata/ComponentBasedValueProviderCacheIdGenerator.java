/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.core.internal.locator.ComponentLocator;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheIdGenerator;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;

import java.util.Optional;

public class ComponentBasedValueProviderCacheIdGenerator implements ValueProviderCacheIdGenerator<ComponentConfiguration> {

  private final DslElementModelFactory elementModelFactory;
  private final DslElementBasedValueProviderCacheIdGenerator delegate;

  public ComponentBasedValueProviderCacheIdGenerator(DslResolvingContext context,
                                                     ComponentLocator<ComponentConfiguration> locator) {
    this.elementModelFactory = DslElementModelFactory.getDefault(context);
    this.delegate = new DslElementBasedValueProviderCacheIdGenerator(location -> locator.get(location)
        .map(c -> elementModelFactory.create(c)
            .orElse(null)));
  }

  @Override
  public Optional<ValueProviderCacheId> getIdForResolvedValues(ComponentConfiguration componentConfiguration,
                                                               String parameterName) {
    checkArgument(componentConfiguration != null, "Cannot generate a Cache Key for a 'null' component");
    return elementModelFactory.create(componentConfiguration)
        .flatMap(m -> delegate.getIdForResolvedValues(m, parameterName));
  }
}
