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
import static org.mule.runtime.core.api.connection.util.ConnectionProviderUtils.unwrapProviderWrapper;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.error;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthManager;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthConnectionProviderWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

import org.slf4j.Logger;
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

  private static Logger LOGGER = getLogger(OAuthOperationMessageProcessor.class);

  private final ExtensionsOAuthManager oauthManager;

  public OAuthOperationMessageProcessor(ExtensionModel extensionModel,
                                        OperationModel operationModel,
                                        ConfigurationProvider configurationProvider,
                                        String target,
                                        String targetValue,
                                        ResolverSet resolverSet,
                                        CursorProviderFactory cursorProviderFactory,
                                        RetryPolicyTemplate retryPolicyTemplate,
                                        ExtensionManager extensionManager,
                                        PolicyManager policyManager,
                                        ExtensionsOAuthManager oauthManager) {
    super(extensionModel, operationModel, configurationProvider, target, targetValue, resolverSet, cursorProviderFactory,
          retryPolicyTemplate, extensionManager, policyManager);
    this.oauthManager = oauthManager;
  }

  @Override
  protected Mono<CoreEvent> doProcess(CoreEvent event, ExecutionContextAdapter<OperationModel> operationContext) {
    return super.doProcess(event, operationContext)
        .onErrorResume(AccessTokenExpiredException.class, e -> {
          OAuthConnectionProviderWrapper connectionProvider = getOAuthConnectionProvider(operationContext);
          if (connectionProvider == null) {
            return error(e);
          }

          AccessTokenExpiredException expiredException = getTokenExpirationException(e);
          if (expiredException != null) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER
                  .debug(format("AccessToken for resourceOwner '%s' expired while executing operation '%s:%s' using config '%s'. "
                      + "Will attempt to refresh token and retry operation", connectionProvider.getResourceOwnerId(),
                                getExtensionModel().getName(), operationContext.getComponentModel().getName(),
                                operationContext.getConfiguration().get().getName()));
            }

            String ownerConfigName = operationContext.getConfiguration().get().getName();
            try {
              oauthManager.refreshToken(ownerConfigName, expiredException.getResourceOwnerId(),
                                        getOAuthConnectionProvider(operationContext));
            } catch (Exception refreshException) {
              return error(new MuleRuntimeException(createStaticMessage(format(
                                                                               "AccessToken for resourceOwner '%s' expired while executing operation '%s:%s' using config '%s'. Refresh token "
                                                                                   + "workflow was attempted but failed with the following exception",
                                                                               connectionProvider.getResourceOwnerId(),
                                                                               getExtensionModel().getName(),
                                                                               operationContext.getComponentModel().getName(),
                                                                               operationContext.getConfiguration().get()
                                                                                   .getName())),
                                                    refreshException));
            }

            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(format("Access Token successfully refreshed for resourceOwnerId '%s' on config '%s'",
                                  connectionProvider.getResourceOwnerId(), operationContext.getConfiguration().get().getName()));
            }

            return super.doProcess(event, operationContext);
          } else {
            return error(e);
          }
        });
  }

  private AccessTokenExpiredException getTokenExpirationException(Exception e) {
    return e instanceof AccessTokenExpiredException
        ? (AccessTokenExpiredException) e
        : (AccessTokenExpiredException) extractCauseOfType(e, AccessTokenExpiredException.class).orElse(null);
  }

  private OAuthConnectionProviderWrapper getOAuthConnectionProvider(ExecutionContextAdapter operationContext) {
    ConfigurationInstance config = ((ConfigurationInstance) operationContext.getConfiguration().get());
    ConnectionProvider provider =
        unwrapProviderWrapper(config.getConnectionProvider().get(), OAuthConnectionProviderWrapper.class);
    return provider instanceof OAuthConnectionProviderWrapper ? (OAuthConnectionProviderWrapper) provider : null;
  }
}
