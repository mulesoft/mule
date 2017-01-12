/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth2.internal.tokenmanager;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.extension.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import org.mule.extension.oauth2.internal.authorizationcode.state.ConfigOAuthContext;
import org.mule.extension.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;
import org.mule.extension.oauth2.internal.tokenmanager.TokenManagerConfig;
import org.mule.test.oauth2.AbstractOAuthAuthorizationTestCase;

import org.junit.Test;

public class InvalidateOauthContextTestCase extends AbstractOAuthAuthorizationTestCase {

  public static final String ACCESS_TOKEN = "Access_token";
  public static final String RESOURCE_OWNER_JOHN = "john";
  public static final String RESOURCE_OWNER_TONY = "tony";

  @Override
  protected String getConfigFile() {
    return "tokenmanager/invalidate-oauth-context-config.xml";
  }

  @Test
  public void invalidateTokenManagerGeneralOauthContext() throws Exception {
    TokenManagerConfig tokenManagerConfig = muleContext.getRegistry().get("tokenManagerConfig");
    initialiseIfNeeded(tokenManagerConfig, muleContext);
    final ConfigOAuthContext configOAuthContext = tokenManagerConfig.getConfigOAuthContext();
    loadResourceOwnerWithAccessToken(configOAuthContext, DEFAULT_RESOURCE_OWNER_ID);
    flowRunner("invalidateOauthContext").withPayload(TEST_MESSAGE).run();
    assertThatOAuthContextWasCleanForUser(configOAuthContext, DEFAULT_RESOURCE_OWNER_ID);
  }

  @Test
  public void invalidateTokenManagerGeneralOauthContextForResourceOwnerId() throws Exception {
    TokenManagerConfig tokenManagerConfig = muleContext.getRegistry().get("tokenManagerConfig");
    initialiseIfNeeded(tokenManagerConfig, muleContext);
    final ConfigOAuthContext configOAuthContext = tokenManagerConfig.getConfigOAuthContext();
    loadResourceOwnerWithAccessToken(configOAuthContext, RESOURCE_OWNER_JOHN);
    loadResourceOwnerWithAccessToken(configOAuthContext, RESOURCE_OWNER_TONY);

    flowRunner("invalidateOauthContextWithResourceOwnerId").withPayload(TEST_MESSAGE)
        .withVariable("resourceOwnerId", RESOURCE_OWNER_TONY).run();
    assertThatOAuthContextWasCleanForUser(configOAuthContext, RESOURCE_OWNER_TONY);
    assertThat(configOAuthContext.getContextForResourceOwner(RESOURCE_OWNER_JOHN).getAccessToken(), is(ACCESS_TOKEN));
  }

  @Test
  public void invalidateTokenManagerForNonExistentResourceOwnerId() throws Exception {
    flowRunner("invalidateOauthContextWithResourceOwnerId").withPayload(TEST_MESSAGE).runExpectingException();
  }

  private void assertThatOAuthContextWasCleanForUser(ConfigOAuthContext configOAuthContext, String resourceOwnerId) {
    assertThat(configOAuthContext.getContextForResourceOwner(resourceOwnerId).getAccessToken(), nullValue());
  }

  private void loadResourceOwnerWithAccessToken(ConfigOAuthContext configOAuthContext, String resourceOwnerId) {
    final ResourceOwnerOAuthContext resourceOwnerContext = configOAuthContext.getContextForResourceOwner(resourceOwnerId);
    resourceOwnerContext.setAccessToken(ACCESS_TOKEN);
    configOAuthContext.updateResourceOwnerOAuthContext(resourceOwnerContext);
  }
}
