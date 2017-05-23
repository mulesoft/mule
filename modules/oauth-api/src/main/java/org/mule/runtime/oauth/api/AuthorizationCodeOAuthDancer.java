/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api;

import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.server.async.HttpResponseReadyCallback;
import org.mule.runtime.oauth.api.exception.RequestAuthenticationException;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.util.concurrent.CompletableFuture;

/**
 * Implementations provide OAuth dance support for authorization-code grant-type.
 *
 * @since 4.0
 */
public interface AuthorizationCodeOAuthDancer {

  /**
   * Will query the internal state (the {@code tokensStore} parameter passed to the service to build the
   * {@link AuthorizationCodeOAuthDancer}) to get the appropriate accessToken. This requires that the authorization has been
   * performed beforehand, otherwise, a {@link RequestAuthenticationException} will be thrown.
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

  /**
   * Clears the oauth context for a given user.
   *
   * @param resourceOwnerId id of the user.
   */
  void invalidateContext(String resourceOwnerId);

  /**
   * Retrieves the oauth context for a particular user. If there's no state for that user a new state is retrieved so never
   * returns null.
   *
   * @param resourceOwnerId id of the user.
   * @return oauth state
   */
  ResourceOwnerOAuthContext getContextForResourceOwner(final String resourceOwnerId);

  /**
   * Handles an http request that will redirect to the access page in {@code authorizationUrl} with the configured credentials.
   * 
   * @param request the request from the user to login and/or authorize the application
   * @param responseCallback the callback where the response with the redirection to the login/authorization page will be sent
   */
  void handleLocalAuthorizationRequest(HttpRequest request, HttpResponseReadyCallback responseCallback);

}
