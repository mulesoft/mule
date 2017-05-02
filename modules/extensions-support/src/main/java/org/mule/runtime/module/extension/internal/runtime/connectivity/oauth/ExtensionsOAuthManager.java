/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

import java.util.Optional;

/**
 * Manages all the resources needed for extensions to consume OAuth providers.
 * <p>
 * Among other things, it manages access token callbacks, authorization endpoints, etc.
 *
 * @since 4.0
 */
public interface ExtensionsOAuthManager {

  /**
   * Becomes aware of the given {@code config} and makes sure that the access token callback
   * and authorization endpoints are provisioned.
   *
   * @param config an {@link OAuthConfig}
   */
  void register(OAuthConfig config) throws MuleException;

  /**
   * Invalidates the OAuth information of a particular resourceOwnerId
   *
   * @param ownerConfigName the name of the extension config which obtained the token
   * @param resourceOwnerId the id of the user to be invalidated
   */
  void invalidate(String ownerConfigName, String resourceOwnerId);

  /**
   * Performs the refresh token flow
   *
   * @param ownerConfigName    the name of the extension config which obtained the token
   * @param resourceOwnerId    the id of the user to be invalidated
   * @param connectionProvider the {@link OAuthConnectionProviderWrapper} which produces the connections
   */
  void refreshToken(String ownerConfigName, String resourceOwnerId, OAuthConnectionProviderWrapper connectionProvider);

  /**
   * @param config an {@link OAuthConfig}
   * @return the {@link ResourceOwnerOAuthContext} for the given {@code config} or {@link Optional#empty()}
   * if authorization hasn't yet taken place or has been invalidated
   */
  Optional<ResourceOwnerOAuthContext> getOAuthContext(OAuthConfig config);
}
