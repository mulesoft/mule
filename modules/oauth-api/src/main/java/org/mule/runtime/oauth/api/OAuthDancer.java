/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api;

import org.mule.runtime.oauth.api.exception.RequestAuthenticationException;

/**
 * Implementations provide OAuth dance support for a specific grant-type.
 *
 * @since 4.0
 */
public interface OAuthDancer {

  /**
   * 
   * @param resourceOwner The resource owner to get the token for.
   * @return the token to send on the authorized request.
   */
  String accessToken(String resourceOwner) throws RequestAuthenticationException;

  /**
   * Performs the refresh of the access token in an non-blocking way, calling the corresponding method in the {@code callback}
   * (?)when {@code refreshCondition} returns {@code true}.
   * 
   * @param callback
   */
  void refreshToken(String resourceOwner, TokenRefreshCallback callback);

  interface TokenRefreshCallback {

    void onSuccess();

    void onError();
  }
}
