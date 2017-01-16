/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.clientcredentials.functional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;

import org.mule.extension.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;
import org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig;

import org.junit.Test;

public class ClientCredentialsNoTokenManagerConfigTestCase extends AbstractClientCredentialsBasicTestCase {

  @Test
  public void authenticationIsDoneOnStartup() throws Exception {
    verifyRequestDoneToTokenUrlForClientCredentials();

    final TokenManagerConfig tokenManagerConfig = muleContext.getRegistry().get("tokenManagerConfig");
    final ResourceOwnerOAuthContext oauthContext = tokenManagerConfig.getConfigOAuthContext()
        .getContextForResourceOwner(DEFAULT_RESOURCE_OWNER_ID);
    assertThat(oauthContext.getAccessToken(), is(ACCESS_TOKEN));
  }

}
