/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.state;

import java.util.Map;

/**
 * OAuth state for a particular resource owner, which typically represents an user.
 * 
 * @since 4.0
 */
public interface ResourceOwnerOAuthContext {

  String DEFAULT_RESOURCE_OWNER_ID = "default";

  /**
   * @return access token of the oauth context retrieved by the token request
   */
  String getAccessToken();

  /**
   * @return refresh token of the oauth context retrieved by the token request
   */
  String getRefreshToken();

  /**
   * @return state of the oauth context send in the authorization request
   */
  String getState();

  /**
   * @return expires in value retrieved by the token request.
   */
  String getExpiresIn();

  /**
   * @return custom token request response parameters configured for extraction.
   */
  Map<String, Object> getTokenResponseParameters();

  /**
   * @return id for the oauth state.
   */
  String getResourceOwnerId();

}
