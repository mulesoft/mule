/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.oauth.internal;

public final class OAuthConstants {

  // Parameters
  public static final String GRANT_TYPE_PARAMETER = "grant_type";
  public static final String REDIRECT_URI_PARAMETER = "redirect_uri";
  public static final String CLIENT_SECRET_PARAMETER = "client_secret";
  public static final String CODE_PARAMETER = "code";
  public static final String CLIENT_ID_PARAMETER = "client_id";
  public static final String ACCESS_TOKEN_PARAMETER = "access_token";
  public static final String EXPIRES_IN_PARAMETER = "expires_in";
  public static final String REFRESH_TOKEN_PARAMETER = "refresh_token";
  public static final String STATE_PARAMETER = "state";
  public static final String SCOPE_PARAMETER = "scope";

  // Values
  public static final String GRANT_TYPE_AUTHENTICATION_CODE = "authorization_code";
  public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
  public static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

  private OAuthConstants() {
    // Nothing to do
  }
}
