/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static java.util.Optional.empty;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

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
 * @since 4.0
 */
public class LazyOperationContext implements ExecutionContext<OperationModel> {

  private final Map<String, ValueResolver> valueResolvers;
  private final OperationModel operationModel;
  private final ExtensionModel extensionModel;
  private final Event event;

  public LazyOperationContext(ResolverSet resolverSet, OperationModel operationModel, ExtensionModel extensionModel,
                              Event event) {

    this.valueResolvers = resolverSet.getResolvers();
    this.operationModel = operationModel;
    this.extensionModel = extensionModel;
    this.event = event;
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
      try {
        return (T) valueResolvers.get(parameterName).resolve(event);
      } catch (MuleException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new NoSuchElementException();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T getTypeSafeParameter(String parameterName, Class<? extends T> expectedType) {
    Object value = getParameter(parameterName);
    if (value == null) {
      return null;
    }

    if (!expectedType.isInstance(value)) {
      throw new IllegalArgumentException(String.format("'%s' was expected to be of type '%s' but type '%s' was found instead",
                                                       parameterName, expectedType.getName(), value.getClass().getName()));
    }

    return (T) value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ConfigurationInstance> getConfiguration() {
    return empty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OperationModel getComponentModel() {
    return operationModel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ExtensionModel getExtensionModel() {
    return extensionModel;
  }
}
