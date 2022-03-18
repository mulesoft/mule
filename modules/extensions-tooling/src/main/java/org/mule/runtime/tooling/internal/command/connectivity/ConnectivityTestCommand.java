/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tooling.internal.command.connectivity;

import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.tooling.internal.util.SdkToolingUtils.stopAndDispose;
import static org.mule.runtime.tooling.internal.util.SdkToolingUtils.toResolverSet;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.module.extension.internal.runtime.config.ConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.tooling.internal.ToolingExpressionManager;
import org.mule.runtime.tooling.internal.command.SdkToolingCommand;
import org.mule.runtime.tooling.internal.command.SdkToolingContext;
import org.mule.runtime.tooling.internal.connectivity.ToolingConnectionProviderBuilder;

import org.slf4j.Logger;

public class ConnectivityTestCommand implements SdkToolingCommand<ConnectionValidationResult> {

  private static final Logger LOGGER = getLogger(ConnectivityTestCommand.class);

  private final ConnectionProviderModel connectionProviderModel;
  private final ReflectionCache reflectionCache = new ReflectionCache();

  public ConnectivityTestCommand(ConnectionProviderModel connectionProviderModel) {
    this.connectionProviderModel = connectionProviderModel;
  }

  @Override
  public ConnectionValidationResult execute(SdkToolingContext context) throws Exception {
    ConnectionProvider<Object> connectionProvider = null;
    Object connection = null;
    try {
      connectionProvider = createConnectionProvider(context);
      connection = connectionProvider.connect();
      return connectionProvider.validate(connection);
    } catch (Exception e) {
      return failure("Failed to perform connectivity test", e);
    } finally {
      if (connectionProvider != null) {
        if (connection != null) {
          connectionProvider.disconnect(connection);
        }
        stopAndDispose(connectionProvider);
      }
    }
  }

  private ConnectionProvider<Object> createConnectionProvider(SdkToolingContext context) throws Exception {
    final ExpressionManager expressionManager = new ToolingExpressionManager();
    final MuleContext muleContext = context.getMuleContext();

    ResolverSet resolverSet = toResolverSet(context.getParameters(),
                                            connectionProviderModel,
                                            muleContext,
                                            reflectionCache,
                                            expressionManager);

    resolverSet.initialise();

    ResolverSetResult result = resolverSet.resolve(ValueResolvingContext.builder(getInitialiserEvent(muleContext)).build());

    ConnectionProviderObjectBuilder<Object> objectBuilder = new ToolingConnectionProviderBuilder(connectionProviderModel,
                                                                                                 resolverSet,
                                                                                                 context.getExtensionModel(),
                                                                                                 expressionManager,
                                                                                                 muleContext);

    ConnectionProvider<Object> connectionProvider = objectBuilder.build(result).getFirst();

    try {
      initialiseIfNeeded(connectionProvider, true, muleContext);
    } catch (InitialisationException e) {
      disposeIfNeeded(connectionProvider, LOGGER);
      throw e;
    }
    startIfNeeded(connectionProvider);

    return connectionProvider;
  }
}
