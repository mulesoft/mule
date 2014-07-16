/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.processor;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.security.oauth.OAuth1Adapter;
import org.mule.security.oauth.notification.OAuthAuthorizeNotification;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OAuth1FetchAccessTokenProcessorTestCase
{

    private static final String accessTokenUrl = "accessTokenUrl";
    private static final String redirectUri = "redirectUri";
    private static final String requestTokenUrl = "requestTokenUrl";
    private static final String authorizationUrl = "authorizationUrl";

    @Mock
    private OAuth1Adapter adapter;
    
    @Mock
    private MuleContext muleContext;

    private MuleEvent event;

    private OAuth1FetchAccessTokenMessageProcessor processor;

    @Before
    public void setUp()
    {
        this.event = Mockito.mock(MuleEvent.class, Mockito.RETURNS_DEEP_STUBS);
        
        this.processor = new OAuth1FetchAccessTokenMessageProcessor(this.adapter);
        this.processor.setAccessTokenUrl(accessTokenUrl);
        this.processor.setRedirectUri(redirectUri);
        this.processor.setAuthorizationUrl(authorizationUrl);
        this.processor.setRequestTokenUrl(requestTokenUrl);
        this.processor.setMuleContext(this.muleContext);
    }

    @Test
    public void process() throws Exception
    {
        final String verifier = "my verifier";
        Mockito.when(event.getMessage().getInvocationProperty("_oauthVerifier")).thenReturn(verifier);
        
        this.processor.process(this.event);
        
        Mockito.verify(this.adapter).fetchAccessToken(requestTokenUrl, accessTokenUrl, authorizationUrl, redirectUri);
        Mockito.verify(this.adapter).setOauthVerifier(verifier);
        
        Mockito.verify(this.muleContext).fireNotification(
            Mockito.argThat(new OAuthNotificationMatcher(
                OAuthAuthorizeNotification.OAUTH_AUTHORIZATION_END, this.event)));
    }

}
