/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractCauseOfType;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.TargetType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.extension.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthManager;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthConnectionProviderWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * A specialization of {@link OperationMessageProcessor} for operations which might be running
 * with an OAuth enabled {@link ConnectionProvider}.
 * <p>
 * If handles {@link AccessTokenExpiredException}s and executes the refresh token flow
 * and retries accordingly.
 * <p>
 * If the operation was not configured with an OAuth enabled connection provider, then it
 * behaves the same as its parent class
 *
 * @since 4.0
 */
public class OAuthOperationMessageProcessor extends OperationMessageProcessor {

  private static Logger LOGGER = LoggerFactory.getLogger(OAuthOperationMessageProcessor.class);

  private final ExtensionsOAuthManager oauthManager;

  public OAuthOperationMessageProcessor(ExtensionModel extensionModel,
                                        OperationModel operationModel,
                                        ConfigurationProvider configurationProvider,
                                        String target,
                                        TargetType targetType,
                                        ResolverSet resolverSet,
                                        CursorProviderFactory cursorProviderFactory,
                                        ExtensionManager extensionManager,
                                        PolicyManager policyManager,
                                        ExtensionsOAuthManager oauthManager) {
    super(extensionModel, operationModel, configurationProvider, target, targetType, resolverSet, cursorProviderFactory,
          extensionManager,
          policyManager);
    this.oauthManager = oauthManager;
  }

  @Override
  protected Mono<Event> doProcess(Event event, ExecutionContextAdapter<OperationModel> operationContext) {
    Optional<OAuthConnectionProviderWrapper> connectionProvider = operationContext.getConfiguration()
        .flatMap(c -> c.getConnectionProvider())
        .filter(cp -> cp instanceof OAuthConnectionProviderWrapper)
        .map(c -> (OAuthConnectionProviderWrapper) c);


    if (connectionProvider.isPresent()) {
      return executeWithOAuthSupport(event, operationContext, connectionProvider.get());
    } else {
      return super.doProcess(event, operationContext);
    }
  }

  private Mono<Event> executeWithOAuthSupport(Event event, ExecutionContextAdapter<OperationModel> operationContext,
                                              OAuthConnectionProviderWrapper connectionProvider) {
    Mono<Event> result = super.doProcess(event, operationContext);
    try {
      //TODO: MULE-12355 - Should not block like this
      return Mono.just(result.block());
    } catch (Exception e) {
      AccessTokenExpiredException expiredException = getTokenExpirationException(e);
      if (expiredException != null) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(format("AccessToken for resourceOwner '%s' expired while executing operation '%s:%s' using config '%s'. "
              + "Will attempt to refresh token and retry operation", connectionProvider.getResourceOwnerId(),
                              getExtensionModel().getName(), operationContext.getComponentModel().getName(),
                              operationContext.getConfiguration().get().getName()));
        }

        String ownerConfigName = operationContext.getConfiguration().get().getName();
        try {
          oauthManager.refreshToken(ownerConfigName, expiredException.getResourceOwnerId(),
                                    getOAuthConnectionProvider(operationContext));
        } catch (Exception refreshException) {
          throw new MuleRuntimeException(createStaticMessage(format(
                                                                    "AccessToken for resourceOwner '%s' expired while executing operation '%s:%s' using config '%s'. Refresh token "
                                                                        + "workflow was attempted but failed with the following exception",
                                                                    connectionProvider.getResourceOwnerId(),
                                                                    getExtensionModel().getName(),
                                                                    operationContext.getComponentModel().getName(),
                                                                    operationContext.getConfiguration().get().getName())),
                                         refreshException);
        }

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(format("Access Token successfully refreshed for resourceOwnerId '%s' on config '%s'",
                              connectionProvider.getResourceOwnerId(), operationContext.getConfiguration().get().getName()));
        }

        result = super.doProcess(event, operationContext);
      } else {
        throw e;
      }
    }

    return result;
  }

  private AccessTokenExpiredException getTokenExpirationException(Exception e) {
    return e instanceof AccessTokenExpiredException
        ? (AccessTokenExpiredException) e
        : (AccessTokenExpiredException) extractCauseOfType(e, AccessTokenExpiredException.class).orElse(null);
  }

  private OAuthConnectionProviderWrapper getOAuthConnectionProvider(ExecutionContextAdapter operationContext) {
    ConfigurationInstance config = ((ConfigurationInstance) operationContext.getConfiguration().get());
    return (OAuthConnectionProviderWrapper) config.getConnectionProvider().get();
  }
}
