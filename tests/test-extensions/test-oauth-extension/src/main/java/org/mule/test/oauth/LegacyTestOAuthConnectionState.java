/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

  // Crear el otro para extender
  // Ver tema de nombres, si este nombre no tendria que ser el de la abstracta
}
