/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

import org.mule.runtime.core.api.Event;

/**
 * It was not possible to retrieve the access token or the refresh token from the token URL response
 */
public class TokenNotFoundException extends Exception {

  private static final long serialVersionUID = -4161482371227766961L;

  private final String tokenResponseAccessToken;
  private final String tokenResponseRefreshToken;
  private final Event tokenUrlResponse;

  public TokenNotFoundException(Event tokenUrlResponse, String accessToken, String refreshToken) {
    this.tokenUrlResponse = tokenUrlResponse;
    this.tokenResponseAccessToken = accessToken;
    this.tokenResponseRefreshToken = refreshToken;
  }

  public Event getTokenUrlResponse() {
    return tokenUrlResponse;
  }

  public String getTokenResponseAccessToken() {
    return tokenResponseAccessToken;
  }

  public String getTokenResponseRefreshToken() {
    return tokenResponseRefreshToken;
  }
}
