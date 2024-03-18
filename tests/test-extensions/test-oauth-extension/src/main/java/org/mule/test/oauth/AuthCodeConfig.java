/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.sdk.api.connectivity.oauth.AuthCodeRequest;

import java.util.LinkedList;
import java.util.List;

@Configuration(name = "auth-code")
@ConnectionProviders({TestOAuthConnectionProvider.class, ScopelessOAuthConnectionProvider.class,
    TestOAuthRefreshValidationConnectionProvider.class, TestWithOAuthParamsConnectionProvider.class,
    TestAuthorizationCodeWithCredentialsPlacementConnectionProvider.class,
    TestAuthorizationCodeDoNotIncludeRedirectUriParamConnectionProvider.class, SdkTestOAuthConnectionProvider.class,
    SdkTestOAuthClientCredentialProvider.class})
@Operations({TestOAuthOperations.class, CallbackOperations.class})
@Sources({TestOAuthRefreshPollingSource.class, TestOAuthRefreshSource.class})
public class AuthCodeConfig {

  private List<AuthCodeRequest> capturedAuthCodeRequests = new LinkedList<>();
  private List<AuthorizationCodeState> capturedAuthCodeStates = new LinkedList<>();

  public List<AuthCodeRequest> getCapturedAuthCodeRequests() {
    return capturedAuthCodeRequests;
  }

  public List<AuthorizationCodeState> getCapturedAuthCodeStates() {
    return capturedAuthCodeStates;
  }
}
