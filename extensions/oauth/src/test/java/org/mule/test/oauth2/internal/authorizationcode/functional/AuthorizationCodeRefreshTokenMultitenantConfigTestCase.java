/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.authorizationcode.functional;

import static org.mule.service.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;

import org.mule.extension.oauth2.internal.authorizationcode.state.ConfigOAuthContext;
import org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig;
import org.mule.runtime.oauth.api.state.DefaultResourceOwnerOAuthContext;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("MULE-11439: Different values in a connection don't trigger a new connect() for cached providers")
public class AuthorizationCodeRefreshTokenMultitenantConfigTestCase extends AbstractAuthorizationCodeRefreshTokenConfigTestCase {

  public static final String MULTITENANT_OAUTH_CONFIG = "tokenManagerConfig";

  public static final String USER_ID_JOHN = "john";
  public static final String JOHN_ACCESS_TOKEN = "123456789";
  public static final String USER_ID_TONY = "tony";
  public static final String TONY_ACCESS_TOKEN = "abcdefghi";

  @Rule
  public SystemProperty multitenantUser = new SystemProperty("multitenant.user", "john");

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"authorization-code/authorization-code-refresh-token-config-with-resource-owner.xml",
        "operations/operations-config.xml"};
  }

  @Test
  public void afterFailureDoRefreshTokenWithCustomValueWithResourceOwnerId() throws Exception {
    final TokenManagerConfig tokenManagerConfig = muleContext.getRegistry().get(MULTITENANT_OAUTH_CONFIG);
    final ConfigOAuthContext configOAuthContext = tokenManagerConfig.getConfigOAuthContext();

    final DefaultResourceOwnerOAuthContext contextForResourceOwnerTony =
        configOAuthContext.getContextForResourceOwner(USER_ID_TONY);
    contextForResourceOwnerTony.setAccessToken(TONY_ACCESS_TOKEN);
    configOAuthContext.updateResourceOwnerOAuthContext(contextForResourceOwnerTony);

    final DefaultResourceOwnerOAuthContext contextForResourceOwnerJohn =
        configOAuthContext.getContextForResourceOwner(USER_ID_JOHN);
    contextForResourceOwnerJohn.setAccessToken(JOHN_ACCESS_TOKEN);
    configOAuthContext.updateResourceOwnerOAuthContext(contextForResourceOwnerJohn);

    executeRefreshToken("testFlow", MULTITENANT_OAUTH_CONFIG, multitenantUser.getValue(),
                        INTERNAL_SERVER_ERROR.getStatusCode());

    verifyTokenManagerAccessToken(USER_ID_JOHN, REFRESHED_ACCESS_TOKEN);
    verifyTokenManagerState(USER_ID_JOHN, null);
    verifyTokenManagerAccessToken(USER_ID_TONY, TONY_ACCESS_TOKEN);
    verifyTokenManagerState(USER_ID_TONY, null);
  }
}
