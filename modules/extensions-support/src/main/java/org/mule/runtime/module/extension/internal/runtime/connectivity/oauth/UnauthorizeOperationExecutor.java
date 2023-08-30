/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.RESOURCE_OWNER_ID_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ExtensionsOAuthUtils.getOAuthConnectionProvider;
import static org.mule.oauth.client.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

/**
 * Synthetic {@link CompletableComponentExecutor} which invalidates a given user's OAuth context.
 *
 * @since 4.0
 */
public class UnauthorizeOperationExecutor implements CompletableComponentExecutor<ComponentModel> {

  @Override
  public void execute(ExecutionContext<ComponentModel> executionContext, ExecutorCallback callback) {
    String ownerId = executionContext.hasParameter(RESOURCE_OWNER_ID_PARAMETER_NAME)
        ? executionContext.getParameter(RESOURCE_OWNER_ID_PARAMETER_NAME)
        : DEFAULT_RESOURCE_OWNER_ID;

    OAuthConnectionProviderWrapper provider = getOAuthConnectionProvider((ExecutionContextAdapter) executionContext);
    provider.invalidate(ownerId);

    callback.complete(null);
  }
}
