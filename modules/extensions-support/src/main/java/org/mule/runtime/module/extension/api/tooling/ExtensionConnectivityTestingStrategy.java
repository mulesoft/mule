/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.tooling;

import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;

import static java.lang.String.format;

import org.mule.api.annotation.NoExtend;
import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connectivity.ConnectivityTestingStrategy;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;

import jakarta.inject.Inject;

/**
 * Implementation of {@code ConnectivityTestingStrategy} that can do connectivity testing over components creates with extensions
 * API.
 *
 * @since 4.0
 */
@NoExtend
@NoInstantiate
public class ExtensionConnectivityTestingStrategy implements ConnectivityTestingStrategy {

  @Inject
  private MuleContext muleContext;

  @Inject
  private ConnectionManager connectionManager;

  @Inject
  private ExpressionManager expressionManager;

  public ExtensionConnectivityTestingStrategy() {}

  /**
   * Used for testing purposes
   *
   * @param muleContext       a {@link MuleContext}
   * @param connectionManager the {@link ConnectionManager} to use for validating the connection.
   */
  ExtensionConnectivityTestingStrategy(ConnectionManager connectionManager, MuleContext muleContext) {
    this.muleContext = muleContext;
    this.connectionManager = connectionManager;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionValidationResult testConnectivity(Object connectivityTestingObject) {
    CoreEvent initialiserEvent = null;
    try {
      initialiserEvent = getInitialiserEvent(muleContext);
      if (connectivityTestingObject instanceof ConnectionProviderResolver) {
        ConnectionProviderResolver<?> resolver = (ConnectionProviderResolver<?>) connectivityTestingObject;
        try (ValueResolvingContext ctx = ValueResolvingContext.builder(initialiserEvent, expressionManager).build()) {
          ConnectionProvider connectionProvider = resolver.resolve(ctx).getFirst();
          return connectionManager.testConnectivity(connectionProvider);
        }
      } else if (connectivityTestingObject instanceof ConfigurationProvider) {
        ConfigurationProvider configurationProvider = (ConfigurationProvider) connectivityTestingObject;
        ConfigurationInstance configurationInstance = configurationProvider.get(initialiserEvent);
        return connectionManager.testConnectivity(configurationInstance);
      } else {
        throw new MuleRuntimeException(createStaticMessage(
                                                           format("testConnectivity was invoked with an object type %s not supported.",
                                                                  connectivityTestingObject.getClass().getName())));
      }
    } catch (Exception e) {
      return failure("Failed to obtain connectivity testing object", e);
    } finally {
      if (initialiserEvent != null) {
        ((BaseEventContext) initialiserEvent.getContext()).success();
      }
    }
  }

  /**
   * @return true whenever there's a {@code ConfigurationProvider} in the configuration, false otherwise.
   */
  @Override
  public boolean accepts(Object connectivityTestingObject) {
    return connectivityTestingObject instanceof ConnectionProviderResolver
        || connectivityTestingObject instanceof ConfigurationProvider;
  }

}
