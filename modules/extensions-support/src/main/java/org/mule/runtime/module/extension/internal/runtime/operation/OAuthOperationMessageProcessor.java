/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.refreshTokenIfNecessary;
import static reactor.core.publisher.Mono.error;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

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
                                        ReflectionCache reflectionCache,
                                        DefaultExecutionMediator.ValueTransformer valueTransformer) {
    super(extensionModel, operationModel, configurationProvider, target, targetValue, resolverSet, cursorProviderFactory,
          retryPolicyTemplate, extensionManager, policyManager, reflectionCache, valueTransformer);
  }

  @Override
  protected Mono<CoreEvent> doProcess(CoreEvent event, ExecutionContextAdapter<OperationModel> operationContext) {
    return super.doProcess(event, operationContext)
        .onErrorResume(Exception.class, e -> {
          boolean tokenRefreshed;
          try {
            tokenRefreshed = refreshTokenIfNecessary(operationContext, e);
          } catch (Exception refreshException) {
            return error(refreshException);
          }

          if (tokenRefreshed) {
            return super.doProcess(event, operationContext);
          } else {
            return error(e);
          }
        });
  }
}
