/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.security.adapter.SdkAuthenticationHandlerAdapter;
import org.mule.sdk.api.security.AuthenticationHandler;

/**
 * {@link ArgumentResolver} which resolves to a {@link AuthenticationHandler} by delegating into a
 * {@link SecurityContextHandlerArgumentResolver} and adapting the result.
 *
 * @since 4.5.0
 */
public final class SdkSecurityContextHandlerArgumentResolver implements ArgumentResolver<AuthenticationHandler> {

  private final ArgumentResolver<org.mule.runtime.extension.api.security.AuthenticationHandler> authenticationHandlerArgumentResolver =
      new SecurityContextHandlerArgumentResolver();

  @Override
  public AuthenticationHandler resolve(ExecutionContext executionContext) {
    org.mule.runtime.extension.api.security.AuthenticationHandler authenticationHandler =
        authenticationHandlerArgumentResolver.resolve(executionContext);
    return authenticationHandler == null ? null : new SdkAuthenticationHandlerAdapter(authenticationHandler);
  }
}
