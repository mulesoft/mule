/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api;

/**
 * General purpose OAuth constants
 * 
 * @since 4.0
 */
public final class OAuthAuthorizationStatusCode {

  public static final String AUTHORIZATION_STATUS_QUERY_PARAM_KEY = "authorizationStatus";

  public static final int AUTHORIZATION_CODE_RECEIVED_STATUS = 0;
  public static final int NO_AUTHORIZATION_CODE_STATUS = 100;
  public static final int TOKEN_URL_CALL_FAILED_STATUS = 200;
  public static final int TOKEN_NOT_FOUND_STATUS = 201;

  private OAuthAuthorizationStatusCode() {
    // Nothing to do
  }
}
