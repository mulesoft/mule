/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api;

import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.server.async.HttpResponseReadyCallback;

/**
 * Implementations provide OAuth dance support for authorization-code grant-type.
 *
 * @since 4.0
 */
public interface AuthorizationCodeOAuthDancer extends OAuthDancer {

  /**
   * Handles an http request that will redirect to the access page in {@code authorizationUrl} with the configured credentials.
   * 
   * @param request the request from the user to login and/or authorize the application
   * @param responseCallback the callback where the response with the redirection to the login/authorization page will be sent
   */
  void handleLocalAuthorizationRequest(HttpRequest request, HttpResponseReadyCallback responseCallback);

}
