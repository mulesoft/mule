/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getAllConnectionProviders;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultImplicitConnectionProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.config.ImplicitConnectionProviderFactory;

import java.util.function.Function;

/**
 * Uses a {@link ImplicitConnectionProviderFactory} to create an implicit {@link ConnectionProvider}.
 * <p>
 * This is a static {@link ValueResolver}. The {@link ConnectionProvider} is created the first time the
 * {@link #resolve(MuleEvent)} method is invoked on {@code this} instance. Subsequent invokations will return the same instance.
 * <p>
 * This class is thread-safe
 *
 * @since 4.0
 */
public final class ImplicitConnectionProviderValueResolver implements ValueResolver<ConnectionProvider> {

  private ConnectionProvider connectionProvider = null;
  private Function<MuleEvent, ConnectionProvider> delegate;

  public ImplicitConnectionProviderValueResolver(String name, RuntimeConfigurationModel configurationModel) {
    if (getAllConnectionProviders(configurationModel).isEmpty()) {
      // No connection provider to resolve
      delegate = nextEvent -> null;
    } else {
      delegate = event -> {
        synchronized (this) {
          if (connectionProvider == null) {
            connectionProvider =
                new DefaultImplicitConnectionProviderFactory().createImplicitConnectionProvider(name, configurationModel, event);
            delegate = nextEvent -> connectionProvider;
          }
          return connectionProvider;
        }
      };
    }
  }

  @Override
  public ConnectionProvider resolve(MuleEvent event) throws MuleException {
    return delegate.apply(event);
  }


  /**
   * @return {@code false}
   */
  @Override
  public boolean isDynamic() {
    return false;
  }
}
