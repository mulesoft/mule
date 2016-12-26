/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.clientcredentials.functional;

import static org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig.defaultTokenManagerConfigIndex;

import org.mule.extension.oauth2.asserter.OAuthContextFunctionAsserter;
import org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig;

import org.junit.Test;

public class ClientCredentialsMinimalConfigTestCase extends AbstractClientCredentialsBasicTestCase {

  @Test
  public void authenticationIsDoneOnStartup() throws Exception {
    verifyRequestDoneToTokenUrlForClientCredentials();

    final TokenManagerConfig tokenManagerConfig =
        muleContext.getRegistry().get("default-token-manager-config-" + (defaultTokenManagerConfigIndex.get() - 1));

    OAuthContextFunctionAsserter.createFrom(tokenManagerConfig).assertAccessTokenIs(ACCESS_TOKEN);
  }

}
