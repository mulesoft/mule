/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Optional.*;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getAllConnectionProviders;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultImplicitConnectionProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.config.ImplicitConnectionProviderFactory;

import java.util.Optional;
import java.util.function.Function;

/**
 * Uses a {@link ImplicitConnectionProviderFactory} to create an implicit {@link ConnectionProvider}.
 * <p>
 * This is a static {@link ValueResolver}. The {@link ConnectionProvider} is created the first time the {@link #resolve(Event)}
 * method is invoked on {@code this} instance. Subsequent invokations will return the same instance.
 * <p>
 * This class is thread-safe
 *
 * @since 4.0
 */
public final class ImplicitConnectionProviderValueResolver implements ConnectionProviderValueResolver {

  private ConnectionProvider connectionProvider = null;
  private Function<Event, ConnectionProvider> delegate;

  public ImplicitConnectionProviderValueResolver(String name,
                                                 ExtensionModel extensionModel,
                                                 ConfigurationModel configurationModel,
                                                 MuleContext muleContext) {
    if (getAllConnectionProviders(extensionModel, configurationModel).isEmpty()) {
      // No connection provider to resolve
      delegate = nextEvent -> null;
    } else {
      delegate = event -> {
        synchronized (this) {
          if (connectionProvider == null) {
            connectionProvider =
                new DefaultImplicitConnectionProviderFactory().createImplicitConnectionProvider(name,
                                                                                                extensionModel,
                                                                                                configurationModel,
                                                                                                event,
                                                                                                muleContext);
            delegate = nextEvent -> connectionProvider;
          }
          return connectionProvider;
        }
      };
    }
  }

  @Override
  public ConnectionProvider resolve(Event event) throws MuleException {
    return delegate.apply(event);
  }


  /**
   * @return {@code false}
   */
  @Override
  public boolean isDynamic() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ResolverSet> getResolverSet() {
    return empty();
  }
}
