/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.authorizationcode;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.module.http.listener.HttpListenerConnectionManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.apache.http.conn.HttpClientConnectionManager;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

@SmallTest
public class AutoTokenRequestHandlerTestCase extends AbstractMuleTestCase
{

    public static final String VALID_URL = "http://localhost:8080/path";
    private MuleContext mockMuleContext = Mockito.mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    private AuthorizationCodeConfig mockOAuthConfig = Mockito.mock(AuthorizationCodeConfig.class, Answers.RETURNS_DEEP_STUBS.get());
    private HttpListenerConnectionManager mockHttpListenerConnectionManager = Mockito.mock(HttpListenerConnectionManager.class, Answers.RETURNS_DEEP_STUBS.get());

    @Test
    @Ignore
    public void useRequestConfigWhenCreatingRequester() throws Exception
    {
        final AutoAuthorizationCodeTokenRequestHandler autoTokenRequestHandler = new AutoAuthorizationCodeTokenRequestHandler();
        autoTokenRequestHandler.setMuleContext(mockMuleContext);
        autoTokenRequestHandler.setOauthConfig(mockOAuthConfig);
        autoTokenRequestHandler.setTokenUrl(VALID_URL);
        when(mockOAuthConfig.getRedirectionUrl()).thenReturn(VALID_URL);
        when(mockMuleContext.getRegistry().get(HttpListenerConnectionManager.HTTP_LISTENER_CONNECTION_MANAGER)).thenReturn(mockHttpListenerConnectionManager);
        autoTokenRequestHandler.init();
        verify(mockOAuthConfig).getRequestConfig();
    }

}
