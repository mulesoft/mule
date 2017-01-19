/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.authorizationcode;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.operation.ParameterResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles the call to the {@code localAuthorizationUrl} and redirects the user to the oauth authentication server authorization
 * url so the user can grant access to the resources to the mule application.
 */
public class AuthorizationRequestHandler {

  /**
   * Scope required by this application to execute. Scopes define permissions over resources.
   */
  @Parameter
  @Optional
  private String scopes;

  /**
   * State parameter for holding state between the authentication request and the callback done by the oauth authorization server
   * to the redirectUrl.
   */
  @Parameter
  @Optional
  private ParameterResolver<String> state;

  /**
   * Identifier under which the oauth authentication attributes are stored (accessToken, refreshToken, etc).
   * <p>
   * This attribute is only required when the applications needs to access resources from more than one user in the OAuth
   * authentication server.
   */
  @Parameter
  @Optional
  private ParameterResolver<String> localAuthorizationUrlResourceOwnerId;

  /**
   * If this attribute is provided mule will automatically create and endpoint in the host server that the user can hit to
   * authenticate and grant access to the application for his account.
   */
  @Parameter
  private String localAuthorizationUrl;

  /**
   * The oauth authentication server url to authorize the app for a certain user.
   */
  @Parameter
  private String authorizationUrl;

  /**
   * Custom parameters to send to the authorization request url or the oauth authorization sever.
   */
  @Parameter
  @Optional
  @Alias("custom-parameters")
  private Map<String, String> customParameters = new HashMap<>();

  public String getScopes() {
    return scopes;
  }

  public String getLocalAuthorizationUrl() {
    return localAuthorizationUrl;
  }

  public String getAuthorizationUrl() {
    return authorizationUrl;
  }

  public ParameterResolver<String> getState() {
    return state;
  }

  public ParameterResolver<String> getLocalAuthorizationUrlResourceOwnerId() {
    return localAuthorizationUrlResourceOwnerId;
  }

  public Map<String, String> getCustomParameters() {
    return customParameters;
  }

}
