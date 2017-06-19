/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import java.util.Optional;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

/**
 * Creates {@link ConnectionProvider} instances which can be implicitly derived from a given {@link ExtensionModel}.
 *
 * @since 4.0
 */
public interface ImplicitConnectionProviderFactory {

  /**
   * Creates a new {@link ConnectionProvider} based on the {@link ConnectionProviderModel} available to the {@code extensionModel}
   * and {@code configurationModel}
   *
   * @param configName the name of the configuration that will own the returned {@link ConnectionProvider}
   * @param extensionModel the {@link ExtensionModel} which owns the {@code configurationModel}
   * @param configurationModel the model that represents the selected config
   * @param event the {@link Event} that will be used to evaluate any default parameters that requires resolving an expression
   * @param muleContext the Mule node.
   * @param <T> the generic type of the connections that the returned provider produces
   * @return a {@link ConnectionProvider}
   * @throws IllegalArgumentException if the {@code extensionModel} doesn't have any {@link ConnectionProviderModel} which can be
   *         used implicitly
   */
  <T> ConnectionProvider<T> createImplicitConnectionProvider(String configName, Event event);

  /**
   * @return whether the {@link ResolverSet} used for resolving the {@link ConnectionProvider} is dynamic or not.
   */
  boolean isDynamic();

  /**
   * @return the {@link ResolverSet} used for resolving the {@link ConnectionProvider} parameters
   */
  Optional<ResolverSet> getResolverSet();

}
