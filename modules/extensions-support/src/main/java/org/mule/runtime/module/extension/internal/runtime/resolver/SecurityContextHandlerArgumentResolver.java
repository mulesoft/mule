/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
