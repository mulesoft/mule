/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import org.mule.runtime.core.api.util.Reference;
import org.mule.runtime.extension.api.ExtensionWalker;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.connection.HasConnectionProviderModels;
import org.mule.runtime.extension.api.introspection.operation.HasOperationModels;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterizedModel;
import org.mule.runtime.extension.api.introspection.source.HasSourceModels;
import org.mule.runtime.extension.api.introspection.source.SourceModel;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A {@link ExtensionWalker} which assures that each component is visited only once, making it easy to handle the fact that some
 * components such as {@link OperationModel}, {@link SourceModel}, {@link ConnectionProviderModel}, etc, implement the flyweight
 * pattern, which means that the same instance might be present at different levels.
 * <p>
 * The use of this walker makes it unnecessary to manually control if a given component has already been seen.
 *
 * @since 4.0
 */
public abstract class IdempotentExtensionWalker extends ExtensionWalker {

  private Set<Reference<SourceModel>> sources = new HashSet<>();
  private Set<Reference<ParameterModel>> parameters = new HashSet<>();
  private Set<Reference<OperationModel>> operations = new HashSet<>();
  private Set<Reference<ConnectionProviderModel>> connectionProviders = new HashSet<>();

  private <T> boolean isFirstAppearance(Set<Reference<T>> accumulator, T item) {
    return accumulator.add(new Reference<>(item));
  }

  @Override
  public final void onSource(HasSourceModels owner, SourceModel model) {
    doOnce(sources, model, this::onSource);
  }

  @Override
  public final void onParameter(ParameterizedModel owner, ParameterModel model) {
    doOnce(parameters, model, this::onParameter);
  }

  @Override
  public final void onOperation(HasOperationModels owner, OperationModel model) {
    doOnce(operations, model, this::onOperation);
  }

  @Override
  public final void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
    doOnce(connectionProviders, model, this::onConnectionProvider);
  }

  private <T> void doOnce(Set<Reference<T>> accumulator, T item, Consumer<T> delegate) {
    if (isFirstAppearance(accumulator, item)) {
      delegate.accept(item);
    }
  }

  /**
   * Invoked when an {@link ConnectionProviderModel} is found in the traversed {@code extensionModel}.
   * <p>
   * This method will only be invoked once per each found instance
   *
   * @param model the {@link ConnectionProviderModel}
   */
  protected void onConnectionProvider(ConnectionProviderModel model) {}

  /**
   * Invoked when an {@link SourceModel} is found in the traversed {@code extensionModel}.
   * <p>
   * This method will only be invoked once per each found instance
   *
   * @param model the {@link SourceModel}
   */
  protected void onSource(SourceModel model) {}

  /**
   * Invoked when an {@link ParameterModel} is found in the traversed {@code extensionModel}.
   * <p>
   * This method will only be invoked once per each found instance
   *
   * @param model the {@link ParameterModel}
   */
  protected void onParameter(ParameterModel model) {}

  /**
   * Invoked when an {@link OperationModel} is found in the traversed {@code extensionModel}.
   * <p>
   * This method will only be invoked once per each found instance
   *
   * @param model the {@link OperationModel}
   */
  protected void onOperation(OperationModel model) {}

}
