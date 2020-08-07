/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata;

import static java.util.Optional.empty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.core.internal.locator.ComponentLocator;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheIdGenerator;

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
    if (isConnection(containerComponent)) {
      return getConnectionModel(containerComponent)
          .flatMap(connection -> delegate.getIdForResolvedValues(connection, parameterName));
    }
    return elementModelFactory.create((ComponentModel) containerComponent)
        .flatMap(e -> delegate.getIdForResolvedValues(e, parameterName));
  }

  private boolean isConnection(ComponentAst componentAst) {
    return componentAst.getModel(ConnectionProviderModel.class).isPresent();
  }

  private Optional<DslElementModel<ComponentAst>> getConnectionModel(ComponentAst componentAst) {
    if (componentAst instanceof ComponentModel) {
      return elementModelFactory.create(componentAst);
    }
    return empty();
  }
}
