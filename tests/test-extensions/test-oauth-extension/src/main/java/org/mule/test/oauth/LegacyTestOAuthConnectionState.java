/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.oauth;

import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthState;

public class LegacyTestOAuthConnectionState extends TestOAuthConnectionState {

  private AuthorizationCodeState state;

  @Override
  public OAuthState getState() {
    return state;
  }
}
