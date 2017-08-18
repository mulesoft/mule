/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static org.mule.runtime.extension.api.connectivity.oauth.ExtensionOAuthConstants.RESOURCE_OWNER_ID_PARAMETER_NAME;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Synthetic {@link OperationExecutor} which invalidates a given user's OAuth context.
 *
 * @since 4.0
 */
public class UnauthorizeOperationExecutor implements OperationExecutor {

  @Inject
  private ExtensionsOAuthManager oauthManager;

  @Override
  public Publisher<Object> execute(ExecutionContext<OperationModel> executionContext) {
    ConfigurationInstance config = executionContext.getConfiguration().get();
    String ownerId = executionContext.hasParameter(RESOURCE_OWNER_ID_PARAMETER_NAME)
        ? executionContext.getParameter(RESOURCE_OWNER_ID_PARAMETER_NAME)
        : DEFAULT_RESOURCE_OWNER_ID;
    oauthManager.invalidate(config.getName(), ownerId);

    return Mono.empty();
  }
}
