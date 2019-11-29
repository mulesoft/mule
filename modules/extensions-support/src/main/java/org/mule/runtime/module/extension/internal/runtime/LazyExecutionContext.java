/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.util.collection.SmallMap.forSize;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * A lazy version of a {@link ExecutionContext} which resolves the values of the parameters
 * of the operation on demand.
 * The laziness of this class is meant for cases where the resolution of a parameters are wanted to
 * be isolated between each other, so when resolving a parameter value, does not matter if other
 * parameters have invalid values.
 *
 * @param <M> the generic type of of the model which represents the component beign executed
 * @since 4.0
 */
public class LazyExecutionContext<M extends ComponentModel> implements EventedExecutionContext<M> {

  private final Map<String, LazyValue<Object>> valueResolvers;
  private final M componentModel;
  private final ExtensionModel extensionModel;
  private final ValueResolvingContext resolvingContext;

  public LazyExecutionContext(ResolverSet resolverSet, M componentModel, ExtensionModel extensionModel,
                              ValueResolvingContext resolvingContext) {

    this.resolvingContext = resolvingContext;
    this.valueResolvers = getValueResolvers(resolverSet);
    this.componentModel = componentModel;
    this.extensionModel = extensionModel;
  }

  private Map<String, LazyValue<Object>> getValueResolvers(ResolverSet resolverSet) {
    Map<String, LazyValue<Object>> valueResolvers = forSize(resolverSet.getResolvers().size());
    resolverSet.getResolvers().forEach((key, resolver) -> valueResolvers.put(key, lazy(resolver)));

    return valueResolvers;
  }

  private LazyValue<Object> lazy(ValueResolver resolver) {
    return new LazyValue((CheckedSupplier) () -> resolver.resolve(resolvingContext));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasParameter(String parameterName) {
    return valueResolvers.containsKey(parameterName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T getParameter(String parameterName) {
    if (hasParameter(parameterName)) {
      return (T) valueResolvers.get(parameterName).get();
    } else {
      throw new NoSuchElementException();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, Object> getParameters() {
    return unmodifiableMap(valueResolvers.entrySet().stream()
        .collect(toMap(entry -> entry.getKey(), entry -> entry.getValue().get())));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ConfigurationInstance> getConfiguration() {
    return resolvingContext.getConfig();
  }

  @Override
  public CoreEvent getEvent() {
    return resolvingContext.getEvent();
  }

  @Override
  public void changeEvent(CoreEvent updated) {
    resolvingContext.changeEvent(updated);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public M getComponentModel() {
    return componentModel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExtensionModel getExtensionModel() {
    return extensionModel;
  }
}
