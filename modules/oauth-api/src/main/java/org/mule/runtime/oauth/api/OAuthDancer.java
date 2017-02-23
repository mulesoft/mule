/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api;

import org.mule.runtime.oauth.api.exception.RequestAuthenticationException;

import java.util.concurrent.CompletableFuture;

/**
 * Implementations provide OAuth dance support for a specific grant-type.
 *
 * @since 4.0
 */
public interface OAuthDancer {

  /**
   * Will query the internal state (the {@code tokensStore} parameter passed to the service to build the {@link OAuthDancer}) to
   * get the appropriate accessToken. This requires that the authorization has been performed beforehand, otherwise, a
   * {@link RequestAuthenticationException} will be thrown.
   * 
   * @param resourceOwner The resource owner to get the token for.
   * @return a future with the token to send on the authorized request. This will be immediately available unless a refresh has to
   *         be made.
   * @throws RequestAuthenticationException if called for a {@code resourceOwner} that has not yet been authorized.
   */
  CompletableFuture<String> accessToken(String resourceOwner) throws RequestAuthenticationException;

  /**
   * Performs the refresh of the access token.
   * 
   * @param resourceOwner The resource owner to get the token for.
   * 
   * @return a completable future that is complete when the token has been refreshed.
   */
  CompletableFuture<Void> refreshToken(String resourceOwner);
}
