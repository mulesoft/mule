/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultImplicitConnectionProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.config.ImplicitConnectionProviderFactory;

import java.util.Optional;

/**
 * Uses a {@link ImplicitConnectionProviderFactory} to create an implicit {@link ConnectionProvider}.
 * <p>
 * This is a static {@link ValueResolver}. The {@link ConnectionProvider} is created the first time the {@link #resolve(ValueResolvingContext)}
 * method is invoked on {@code this} instance. Subsequent invocations will return the same instance.
 * <p>
 * This class is thread-safe
 *
 * @since 4.0
 */
public final class ImplicitConnectionProviderValueResolver<C> implements ConnectionProviderValueResolver<C> {

  private final ImplicitConnectionProviderFactory implicitConnectionProviderFactory;
  private final String configName;

  public ImplicitConnectionProviderValueResolver(String name,
                                                 ExtensionModel extensionModel,
                                                 ConfigurationModel configurationModel,
                                                 MuleContext muleContext) {
    configName = name;
    implicitConnectionProviderFactory =
        new DefaultImplicitConnectionProviderFactory(extensionModel, configurationModel, muleContext);
  }

  @Override
  public Pair<ConnectionProvider<C>, ResolverSetResult> resolve(ValueResolvingContext context) throws MuleException {
    return implicitConnectionProviderFactory.createImplicitConnectionProvider(configName, context.getEvent());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDynamic() {
    return implicitConnectionProviderFactory.isDynamic();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ResolverSet> getResolverSet() {
    return implicitConnectionProviderFactory.getResolverSet();
  }
}
