/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import org.mule.runtime.extension.api.annotation.connectivity.oauth.OAuthCallbackValue;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.OAuthParameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;

public class TestOAuthConnectionState {

  @Parameter
  @Optional(defaultValue = "34.0")
  private Double apiVersion;

  /**
   * Tailors the login page to the user's device type.
   */
  @OAuthParameter
  private String display;

  /**
   * Avoid interacting with the user
   */
  @OAuthParameter
  @Optional(defaultValue = "false")
  private boolean immediate;

  /**
   * Specifies how the authorization server prompts the user for reauthentication and reapproval
   */
  @OAuthParameter
  @Optional(defaultValue = "true")
  private boolean prompt;

  @OAuthCallbackValue(expression = "#[payload.instance_url]")
  private String instanceId;

  @OAuthCallbackValue(expression = "#[payload.id]")
  private String userId;


  private AuthorizationCodeState state;

  public Double getApiVersion() {
    return apiVersion;
  }

  public String getDisplay() {
    return display;
  }

  public boolean isImmediate() {
    return immediate;
  }

  public boolean isPrompt() {
    return prompt;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getUserId() {
    return userId;
  }

  public AuthorizationCodeState getState() {
    return state;
  }
}
