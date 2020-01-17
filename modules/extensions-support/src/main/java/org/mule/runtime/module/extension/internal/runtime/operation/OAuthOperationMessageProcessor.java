/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.lang.String.format;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ExceptionUtils.extractOfType;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.getOAuthConnectionProvider;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantTypeVisitor;
import org.mule.runtime.extension.api.connectivity.oauth.PlatformManagedOAuthGrantType;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthConnectionProviderWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.Optional;

import org.slf4j.Logger;

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
                                        ReflectionCache reflectionCache) {
    super(extensionModel, operationModel, configurationProvider, target, targetValue, resolverSet, cursorProviderFactory,
          retryPolicyTemplate, extensionManager, policyManager, reflectionCache);
  }

  @Override
  protected void executeOperation(ExecutionContextAdapter<OperationModel> operationContext, ExecutorCallback callback) {
    super.executeOperation(operationContext, refreshable(operationContext, callback));
  }

  private ExecutorCallback refreshable(ExecutionContextAdapter<OperationModel> operationContext, ExecutorCallback callback) {
    return new ExecutorCallback() {

      @Override
      public void complete(Object value) {
        callback.complete(value);
      }

      @Override
      public void error(Throwable e) {
        AccessTokenExpiredException expiredException = getTokenExpirationException(e);
        if (expiredException == null) {
          callback.error(e);
          return;
        }

        OAuthConnectionProviderWrapper connectionProvider = getOAuthConnectionProvider(operationContext);
        if (connectionProvider == null) {
          callback.error(e);
          return;
        }

        Reference<Optional<String>> resourceOwnerIdReference = new Reference<>(Optional.empty());
        connectionProvider.getGrantType().accept(new OAuthGrantTypeVisitor() {

          @Override
          public void visit(AuthorizationCodeGrantType grantType) {
            String rsId = connectionProvider.getResourceOwnerId();
            resourceOwnerIdReference.set(of(rsId));

            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("AccessToken for resourceOwner '{}' expired while executing operation '{}:{}' using config '{}'. "
                  + "Will attempt to refresh token and retry operation",
                           rsId, getExtensionModel().getName(), operationContext.getComponentModel().getName(),
                           operationContext.getConfiguration().get().getName());
            }
          }

          @Override
          public void visit(ClientCredentialsGrantType grantType) {
            logRefresh();
          }

          @Override
          public void visit(PlatformManagedOAuthGrantType grantType) {
            logRefresh();
          }

          private void logRefresh() {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("AccessToken for expired while executing operation '{}:{}' using config '{}'. "
                  + "Will attempt to refresh token and retry operation",
                           getExtensionModel().getName(), operationContext.getComponentModel().getName(),
                           operationContext.getConfiguration().get().getName());
            }
          }
        });

        Optional<String> resourceOwnerId = resourceOwnerIdReference.get();

        try {
          connectionProvider.refreshToken(resourceOwnerId.orElse(""));
        } catch (Exception refreshException) {
          callback.error(new MuleRuntimeException(createStaticMessage(format(
                                                                             "AccessToken %s expired while executing operation '%s:%s' using config '%s'. Refresh token "
                                                                                 + "workflow was attempted but failed with the following exception",
                                                                             forResourceOwner(resourceOwnerId),
                                                                             getExtensionModel().getName(),
                                                                             operationContext.getComponentModel().getName(),
                                                                             operationContext.getConfiguration().get()
                                                                                 .getName())),
                                                  refreshException));

          return;
        }

        LOGGER.debug("Access Token successfully refreshed {} on config '{}'",
                     forResourceOwner(resourceOwnerId),
                     operationContext.getConfiguration().get().getName());

        OAuthOperationMessageProcessor.super.executeOperation(operationContext, callback);
      }
    };
  }

  private String forResourceOwner(Optional<String> resourceOwnerId) {
    return resourceOwnerId.map(id -> format("for resource owner '%s' ", id)).orElse("");
  }

  private AccessTokenExpiredException getTokenExpirationException(Throwable t) {
    return t instanceof AccessTokenExpiredException
        ? (AccessTokenExpiredException) t
        : extractOfType(t, AccessTokenExpiredException.class).orElse(null);
  }
}
