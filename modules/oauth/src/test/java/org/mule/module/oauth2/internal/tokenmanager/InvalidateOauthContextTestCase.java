/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.tokenmanager;

import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.module.oauth2.internal.authorizationcode.state.ConfigOAuthContext;
import org.mule.module.oauth2.internal.authorizationcode.state.ResourceOwnerOAuthContext;
import org.mule.tck.junit4.FunctionalTestCase;

import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class InvalidateOauthContextTestCase extends FunctionalTestCase
{

    public static final String ACCESS_TOKEN = "Access_token";
    public static final String RESOURCE_OWNER_JOHN = "john";
    public static final String RESOURCE_OWNER_TONY = "tony";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    protected String getConfigFile()
    {
        return "tokenmanager/invalidate-oauth-context-config.xml";
    }

    @Test
    public void invalidateTokenManagerGeneralOauthContext() throws Exception
    {
        TokenManagerConfig tokenManagerConfig = muleContext.getRegistry().get("tokenManagerConfig");
        final ConfigOAuthContext configOAuthContext = tokenManagerConfig.getConfigOAuthContext();
        loadResourceOwnerWithAccessToken(configOAuthContext, ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID);
        ((Flow)getFlowConstruct("invalidateOauthContext")).process(getTestEvent(TEST_MESSAGE));
        assertThatOAuthContextWasCleanForUser(configOAuthContext, ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID);
    }

    @Test
    public void invalidateTokenManagerGeneralOauthContextForResourceOwnerId() throws Exception
    {
        TokenManagerConfig tokenManagerConfig = muleContext.getRegistry().get("tokenManagerConfig");
        final ConfigOAuthContext configOAuthContext = tokenManagerConfig.getConfigOAuthContext();
        loadResourceOwnerWithAccessToken(configOAuthContext, RESOURCE_OWNER_JOHN);
        loadResourceOwnerWithAccessToken(configOAuthContext, RESOURCE_OWNER_TONY);
        final MuleEvent testEvent = getTestEvent(TEST_MESSAGE);
        testEvent.setFlowVariable("resourceOwnerId", RESOURCE_OWNER_TONY);
        ((Flow)getFlowConstruct("invalidateOauthContextWithResourceOwnerId")).process(testEvent);
        assertThatOAuthContextWasCleanForUser(configOAuthContext, RESOURCE_OWNER_TONY);
        assertThat(configOAuthContext.getContextForResourceOwner(RESOURCE_OWNER_JOHN).getAccessToken(), Is.is(ACCESS_TOKEN));
    }

    @Test
    public void invalidateTokenManagerForNonExistentResourceOwnerId() throws Exception
    {
        final MuleEvent testEvent = getTestEvent(TEST_MESSAGE);
        expectedException.expect(MessagingException.class);
        ((Flow)getFlowConstruct("invalidateOauthContextWithResourceOwnerId")).process(testEvent);
    }

    private void assertThatOAuthContextWasCleanForUser(ConfigOAuthContext configOAuthContext, String resourceOwnerId)
    {
        assertThat(configOAuthContext.getContextForResourceOwner(resourceOwnerId).getAccessToken(), nullValue());
    }

    private void loadResourceOwnerWithAccessToken(ConfigOAuthContext configOAuthContext, String resourceOwnerId)
    {
        final ResourceOwnerOAuthContext resourceOwnerContext = configOAuthContext.getContextForResourceOwner(resourceOwnerId);
        resourceOwnerContext.setAccessToken(ACCESS_TOKEN);
        configOAuthContext.updateResourceOwnerOAuthContext(resourceOwnerContext);
    }
}
