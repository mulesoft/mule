/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api;

import org.mule.runtime.oauth.api.exception.RequestAuthenticationException;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.util.concurrent.CompletableFuture;

/**
 * Implementations provide OAuth dance support for client-credentials grant-type.
 *
 * @since 4.0
 */
public interface ClientCredentialsOAuthDancer {

  /**
   * Will query the internal state (the {@code tokensStore} parameter passed to the service to build the
   * {@link ClientCredentialsOAuthDancer}) to get the appropriate accessToken. This requires that the authorization has been
   * performed beforehand, otherwise, a {@link RequestAuthenticationException} will be thrown.
   * 
   * @return a future with the token to send on the authorized request. This will be immediately available unless a refresh has to
   *         be made.
   * @throws RequestAuthenticationException if called for a {@code resourceOwner} that has not yet been authorized.
   */
  CompletableFuture<String> accessToken() throws RequestAuthenticationException;

  /**
   * Performs the refresh of the access token.
   * 
   * @return a completable future that is complete when the token has been refreshed.
   */
  CompletableFuture<Void> refreshToken();

  /**
   * Clears the oauth context.
   */
  void invalidateContext();

  /**
   * Retrieves the oauth context. If there's no state, a new state is retrieved so never returns null.
   *
   * @return oauth state
   */
  ResourceOwnerOAuthContext getContext();

}
