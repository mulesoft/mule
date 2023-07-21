/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.security.AuthenticationHandler;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.security.DefaultAuthenticationHandler;

/**
 * An {@link ArgumentResolver} which returns the {@link AuthenticationHandler} of the current event
 *
 * @since 4.0
 */
public final class SecurityContextHandlerArgumentResolver implements ArgumentResolver<AuthenticationHandler> {

  @Override
  public AuthenticationHandler resolve(ExecutionContext executionContext) {
    ExecutionContextAdapter context = ((ExecutionContextAdapter) executionContext);
    return new DefaultAuthenticationHandler(context.getSecurityContext(),
                                            context.getMuleContext().getSecurityManager(),
                                            context::setSecurityContext);
  }
}
