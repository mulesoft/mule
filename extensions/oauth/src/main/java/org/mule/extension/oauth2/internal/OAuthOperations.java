/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

import static java.util.Objects.requireNonNull;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

/**
 * Provides management capabilities for the configured {@code tokenManager}.
 *
 * @since 4.0
 */
public class OAuthOperations {

  /**
   * Clears the oauth context for a token manager and a resource owner id.
   * 
   * @param tokenManager The token manager which holds the credentials to invalidate.
   * @param resourceOwnerId The resource owner id to invalidate. This attribute is only allowed for authorization code grant type.
   */
  public void invalidateOauthContext(TokenManagerConfig tokenManager,
                                     @Optional(defaultValue = DEFAULT_RESOURCE_OWNER_ID) String resourceOwnerId) {
    validateResourceOwnerId(resourceOwnerId);
    tokenManager.getConfigOAuthContext().clearContextForResourceOwner(resourceOwnerId);
  }

  /**
   * Returns the access token of the token manager for the parametrized resource owner ID
   *
   * @param tokenManager The token manager which holds the access token.
   * @param resourceOwnerId The resource owner id to invalidate. This attribute is only allowed for authorization code grant type.
   * @return access token of the oauth context retrieved by the token request.
   */
  public String retrieveAccessToken(TokenManagerConfig tokenManager,
                                    @Optional(defaultValue = DEFAULT_RESOURCE_OWNER_ID) String resourceOwnerId) {
    validateResourceOwnerId(resourceOwnerId);
    return getContextForResourceOwner(tokenManager, resourceOwnerId).getAccessToken();
  }

  /**
   * Returns the refresh token of the oauth context for the parametrized resource owner ID
   *
   * @param tokenManager The token manager which holds the refresh token.
   * @param resourceOwnerId The resource owner id to invalidate. This attribute is only allowed for authorization code grant type.
   * @return refresh token of the oauth context retrieved by the token request.
   */
  public String retrieveRefreshToken(TokenManagerConfig tokenManager,
                                     @Optional(defaultValue = DEFAULT_RESOURCE_OWNER_ID) String resourceOwnerId) {
    return getContextForResourceOwner(tokenManager, resourceOwnerId).getRefreshToken();
  }

  /**
   * Returns the expiration of the oauth context for the parametrized resource owner ID
   *
   * @param tokenManager The token manager which holds the access token.
   * @param resourceOwnerId The resource owner id to invalidate. This attribute is only allowed for authorization code grant type.
   * @return the expiration of the oauth context retrieved by the token request.
   */
  public String retrieveExpiresIn(TokenManagerConfig tokenManager,
                                  @Optional(defaultValue = DEFAULT_RESOURCE_OWNER_ID) String resourceOwnerId) {
    return getContextForResourceOwner(tokenManager, resourceOwnerId).getExpiresIn();
  }

  /**
   * Returns the state of the oauth context for the parametrized resource owner ID
   *
   * @param tokenManager The token manager which holds the access token.
   * @param resourceOwnerId The resource owner id to invalidate. This attribute is only allowed for authorization code grant type.
   * @return state of the oauth context retrieved by the token request.
   */
  public String retrieveState(TokenManagerConfig tokenManager,
                              @Optional(defaultValue = DEFAULT_RESOURCE_OWNER_ID) String resourceOwnerId) {
    return getContextForResourceOwner(tokenManager, resourceOwnerId).getState();
  }

  /**
   * Returns the value of the parameter that was extracted during the dance from the token manager for the parametrized resource owner ID
   *
   * @param tokenManager The token manager which holds the access token.
   * @param resourceOwnerId The resource owner id to invalidate. This attribute is only allowed for authorization code grant type.
   * @param key to look for in the elements that has been extracted after the previous OAuth dance.
   * @return an element if there was previously introduced by the OAuth dance and the custom parameter extractor. Null otherwise
   * @see AbstractGrantType#parameterExtractors
   */
  @OutputResolver(output = TokenResponseParameterOutputResolver.class)
  public Object retrieveCustomTokenResponseParam(TokenManagerConfig tokenManager,
                                                 @Optional(defaultValue = DEFAULT_RESOURCE_OWNER_ID) String resourceOwnerId,
                                                 String key) {
    return getContextForResourceOwner(tokenManager, resourceOwnerId).getTokenResponseParameters().get(key);
  }

  private void validateResourceOwnerId(String resourceOwnerId) {
    requireNonNull(resourceOwnerId, "Resource owner id cannot be null");
  }

  private ResourceOwnerOAuthContext getContextForResourceOwner(TokenManagerConfig tokenManager, String resourceOwnerId) {
    validateResourceOwnerId(resourceOwnerId);
    return tokenManager.getConfigOAuthContext().getContextForResourceOwner(resourceOwnerId);
  }
}
