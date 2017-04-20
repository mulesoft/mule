/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api;

import java.util.Optional;

/**
 * Provides information about a request to be made to fetch the tokens as part of an OAuth Authorization-code grant type dance.
 * 
 * @since 4.0
 */
public interface AuthorizationCodeRequest {

  /**
   * @return id for the oauth state.
   */
  String getResourceOwnerId();

  /**
   * @return The OAuth authentication server url that authorized the app for a certain user.
   */
  String getAuthorizationUrl();

  /**
   * @return The OAuth authentication server url that provided the tokens.
   */
  String getTokenUrl();

  /**
   * @return The application identifier as defined in the OAuth authentication server.
   */
  String getClientId();

  /**
   * @return The application secret as defined in the OAuth authentication server.
   */
  String getClientSecret();

  /**
   * @return the scopes sent to the authorization url.
   */
  String getScopes();

  /**
   * @return state kept between the authentication request and the callback done by the OAuth authorization server to the external
   *         callback url.
   */
  Optional<String> getState();
}
