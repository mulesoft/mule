/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.oauth2.internal.state.OAuthStateRegistry;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

@SmallTest
public class AuthorizationCodeConfigTestCase extends AbstractMuleTestCase
{

    public static final String TEST_CONFIG_NAME = "testConfigName";
    private MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    private OAuthStateRegistry mockAuthStateRegistry = mock(OAuthStateRegistry.class, Answers.RETURNS_DEEP_STUBS.get());

    @Test
    public void onInitializationRegisterOAuthState() throws Exception
    {
        final AuthorizationCodeConfig authorizationCodeConfig = createInitializedConfig();
        verify(mockAuthStateRegistry).registerOAuthState(TEST_CONFIG_NAME, authorizationCodeConfig.getOAuthState());
    }

    @Test
    public void onDisposeUnegisterOAuthState() throws Exception
    {
        final AuthorizationCodeConfig authorizationCodeConfig = createInitializedConfig();
        authorizationCodeConfig.dispose();
        verify(mockAuthStateRegistry).unregisterOAuthState(TEST_CONFIG_NAME);
    }

    private AuthorizationCodeConfig createInitializedConfig() throws org.mule.api.registry.RegistrationException, InitialisationException
    {
        final AuthorizationCodeConfig authorizationCodeConfig = new AuthorizationCodeConfig();
        Mockito.when(mockMuleContext.getRegistry().lookupObject(OAuthStateRegistry.class)).thenReturn(mockAuthStateRegistry);
        authorizationCodeConfig.setMuleContext(mockMuleContext);
        authorizationCodeConfig.setName(TEST_CONFIG_NAME);
        authorizationCodeConfig.initialise();
        return authorizationCodeConfig;
    }

}
