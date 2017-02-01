/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal;

import static org.mule.extension.http.api.error.HttpError.SECURITY;
import org.mule.extension.http.api.listener.HttpBasicAuthenticationFilter;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.security.AuthenticationHandler;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Errors that can be thrown in the {@link HttpOperations#basicSecurityFilter(HttpBasicAuthenticationFilter, AuthenticationHandler)}
 * operation.
 *
 * @since 4.0
 */
public class BasicSecurityErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    //TODO: MULE-11469 - Throw UNAUTHORISED directly
    return ImmutableSet.<ErrorTypeDefinition>builder().add(SECURITY).build();
  }
}
