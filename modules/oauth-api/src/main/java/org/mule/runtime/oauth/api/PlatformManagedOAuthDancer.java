/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoImplement;
import org.mule.runtime.oauth.api.builder.OAuthPlatformManagedDancerBuilder;
import org.mule.runtime.oauth.api.listener.PlatformManagedOAuthStateListener;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.util.concurrent.CompletableFuture;

/**
 * Allows to manipulate access and refresh tokens that are obtained and managed by the Anypoint Platform. Each instance is
 * preconfigured by an {@link OAuthPlatformManagedDancerBuilder} to point to a specific connection URI.
 * <p>
 * Since the authorizations are performed by the platform, this dancer remains agnostic of the grant type that was used to
 * obtain them.
 * <p>
 * Platform Managed OAuth is an experimental feature. It will only be enabled on selected environments and scenarios.
 * Backwards compatibility is not guaranteed.
 *
 * @since 4.3.0
 */
@NoImplement
@Experimental
public interface PlatformManagedOAuthDancer {

  /**
   * Obtains the current access token for the connection that this dancer was configured for.
   *
   * @return a future with the token to send on the authorized request.
   */
  CompletableFuture<String> accessToken();

  /**
   * Performs the refresh of the access token.
   *
   * @return a completable future that is complete when the token has been refreshed.
   */
  CompletableFuture<Void> refreshToken();

  /**
   * Obtains a {@link PlatformManagedConnectionDescriptor} which describes the connection this dancer accesses
   * @return a {@link CompletableFuture} which returns a {@link PlatformManagedConnectionDescriptor}
   */
  CompletableFuture<PlatformManagedConnectionDescriptor> getConnectionDescriptor();

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

  /**
   * Adds the {@code listener}. Listeners will be invoked in the same order as they were added
   *
   * @param listener the {@link PlatformManagedOAuthStateListener} to be added
   * @throws IllegalArgumentException if the {@code listener} is {@code null}
   */
  void addListener(PlatformManagedOAuthStateListener listener);

  /**
   * Removes the {@code listener}. Nothing happens if it wasn't part of {@code this} dancer.
   *
   * @param listener the {@link PlatformManagedOAuthStateListener} to be removed
   * @throws IllegalArgumentException if the {@code listener} is {@code null}
   */
  void removeListener(PlatformManagedOAuthStateListener listener);
}
