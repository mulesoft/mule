/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

import static java.util.Objects.requireNonNull;
import static org.mule.extension.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;

import org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig;
import org.mule.runtime.extension.api.annotation.param.Optional;

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
    requireNonNull(resourceOwnerId, "Resource owner id cannot be null");
    tokenManager.getConfigOAuthContext().clearContextForResourceOwner(resourceOwnerId);
  }
}
