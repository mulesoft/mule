/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.MAX_REFRESH_ATTEMPTS;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.refreshTokenIfNecessary;
import static org.mule.runtime.module.extension.internal.runtime.streaming.CursorResetInterceptor.CURSOR_RESET_HANDLER_VARIABLE;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.exception.EnrichedErrorMapping;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.extension.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.streaming.CursorResetHandler;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.List;

/**
 * A specialization of {@link OperationMessageProcessor} for operations which might be running with an OAuth enabled
 * {@link ConnectionProvider}.
 * <p>
 * If handles {@link AccessTokenExpiredException}s and executes the refresh token flow and retries accordingly.
 * <p>
 * If the operation was not configured with an OAuth enabled connection provider, then it behaves the same as its parent class
 *
 * @since 4.0
 */
public class OAuthOperationMessageProcessor extends OperationMessageProcessor {

  public OAuthOperationMessageProcessor(ExtensionModel extensionModel,
                                        OperationModel operationModel,
                                        ConfigurationProvider configurationProvider,
                                        String target,
                                        String targetValue,
                                        List<EnrichedErrorMapping> errorMappings,
                                        ResolverSet resolverSet,
                                        CursorProviderFactory cursorProviderFactory,
                                        RetryPolicyTemplate retryPolicyTemplate,
                                        MessageProcessorChain nestedChain,
                                        ExtensionManager extensionManager,
                                        PolicyManager policyManager,
                                        ReflectionCache reflectionCache,
                                        DefaultExecutionMediator.ResultTransformer resultTransformer,
                                        long outerFluxTerminationTimeout) {
    super(extensionModel, operationModel, configurationProvider, target, targetValue, errorMappings, resolverSet,
          cursorProviderFactory, retryPolicyTemplate, nestedChain,
          extensionManager, policyManager, reflectionCache, resultTransformer, outerFluxTerminationTimeout);
  }

  @Override
  protected void executeOperation(ExecutionContextAdapter<OperationModel> operationContext, ExecutorCallback callback) {
    super.executeOperation(operationContext, refreshable(operationContext, callback));
  }

  private ExecutorCallback refreshable(ExecutionContextAdapter<OperationModel> operationContext, ExecutorCallback callback) {
    return new ExecutorCallback() {

      private int attempts = 0;

      @Override
      public void complete(Object value) {
        callback.complete(value);
      }

      @Override
      public void error(Throwable e) {
        try {
          if (++attempts <= MAX_REFRESH_ATTEMPTS && refreshTokenIfNecessary(operationContext, e)) {
            resetCursors(operationContext);
            OAuthOperationMessageProcessor.super.executeOperation(operationContext, this);
          } else {
            callback.error(e);
          }
        } catch (Exception refreshException) {
          callback.error(refreshException);
        }
      }

      private void resetCursors(ExecutionContextAdapter<OperationModel> operationContext) {
        CursorResetHandler cursorResetHandler =
            ((ExecutionContextAdapter<OperationModel>) operationContext).getVariable(CURSOR_RESET_HANDLER_VARIABLE);
        if (cursorResetHandler != null) {
          cursorResetHandler.resetCursors();
        }
      }
    };
  }
}
