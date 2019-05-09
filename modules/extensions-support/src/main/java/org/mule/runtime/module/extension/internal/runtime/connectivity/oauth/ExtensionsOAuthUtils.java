/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;

/**
 * Utility methods for the OAuth support on the SDK
 *
 * @since 4.0
 */
public final class ExtensionsOAuthUtils {

  public static AuthorizationCodeState toAuthorizationCodeState(AuthorizationCodeConfig config, ResourceOwnerOAuthContext context) {
    return new ImmutableAuthorizationCodeState(context.getAccessToken(),
                                               context.getRefreshToken(),
                                               context.getResourceOwnerId(),
                                               context.getExpiresIn(),
                                               context.getState(),
                                               config.getAuthorizationUrl(),
                                               config.getAccessTokenUrl(),
                                               config.getCallbackConfig().getExternalCallbackUrl(),
                                               config.getConsumerKey(),
                                               config.getConsumerSecret());
  }

  private ExtensionsOAuthUtils() {}
}
