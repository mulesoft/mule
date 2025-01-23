/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OAuthCallbackConfigTest {

  private OAuthCallbackConfig oAuthConfig;

  private static final String LISTENER_CONFIG = "listenerConfig";
  private static final String CALLBACK_PATH = "callbackPath";
  private static final String LOCAL_AUTHORIZE_PATH = "localAuthorizePath";
  private static final String EXTERNAL_CALLBACK_URL = "externalCallbackUrl";

  @Test
  public void testGetConsumerKey() {
    oAuthConfig = new OAuthCallbackConfig(LISTENER_CONFIG, CALLBACK_PATH, LOCAL_AUTHORIZE_PATH, EXTERNAL_CALLBACK_URL);

    assertEquals(oAuthConfig.getListenerConfig(), LISTENER_CONFIG);
    assertEquals(oAuthConfig.getCallbackPath(), CALLBACK_PATH);
    assertEquals(oAuthConfig.getLocalAuthorizePath(), LOCAL_AUTHORIZE_PATH);
    assertEquals(oAuthConfig.getExternalCallbackUrl().get(), EXTERNAL_CALLBACK_URL);
  }
}
