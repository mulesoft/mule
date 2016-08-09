/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.module.extension.internal.introspection.utils.ImplicitObjectUtils;
import org.mule.runtime.module.extension.internal.util.MuleExtensionUtils;

import java.util.List;

/**
 * Creates {@link ConnectionProvider} instances which can be implicitly derived from a given {@link ExtensionModel}.
 *
 * @since 4.0
 */
public interface ImplicitConnectionProviderFactory {

  /**
   * Creates a new {@link ConnectionProvider} based on the {@link ConnectionProviderModel} obtained from invoking
   * {@link ImplicitObjectUtils#getFirstImplicit(List)} on all the providers available to {@code configurationModel} (under the
   * rules of {@link MuleExtensionUtils#getAllConnectionProviders(RuntimeConfigurationModel)})
   *
   * @param configName the name of the configuration that will own the returned {@link ConnectionProvider}
   * @param configurationModel the model that represents the selected config
   * @param event the {@link MuleEvent} that will be used to evaluate any default parameters that requires resolving an expression
   * @param <Connector> the generic type of the connections that the returned provider produces
   * @return a {@link ConnectionProvider}
   * @throws IllegalArgumentException if the {@code extensionModel} doesn't have any {@link ConnectionProviderModel} which can be
   *         used implicitly
   */
  <Connector> ConnectionProvider<Connector> createImplicitConnectionProvider(String configName,
                                                                             RuntimeConfigurationModel configurationModel,
                                                                             MuleEvent event);
}
