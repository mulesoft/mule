/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth.processor;

import org.mule.api.MuleEvent;
import org.mule.security.oauth.OAuth2Adapter;
import org.mule.security.oauth.OAuth2Manager;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OAuth2FetchAccessTokenProcessorTestCase
{

    private static final String verifier = "verifier";

    private OAuth2Manager<OAuth2Adapter> manager = null;
    private OAuth2FetchAccessTokenMessageProcessor processor;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp()
    {
        this.manager = Mockito.mock(OAuth2Manager.class, Mockito.RETURNS_DEEP_STUBS);
        this.processor = new OAuth2FetchAccessTokenMessageProcessor(this.manager);
    }

    @Test
    public void adapterWithUrl() throws Exception
    {
        final String accessTokenUrl = "accessTokenUrl";
        final String redirectUri = "redirectUri";
        final String accessTokenId = "accessTokenId";

        this.processor.setRedirectUri(redirectUri);

        OAuth2Adapter adapter = Mockito.mock(OAuth2Adapter.class);

        Mockito.when(this.manager.createAdapter(verifier)).thenReturn(adapter);
        Mockito.when(adapter.getAccessTokenUrl()).thenReturn(accessTokenUrl);
        Mockito.when(adapter.getAccessTokenId()).thenReturn(accessTokenId);

        MuleEvent event = this.getEvent();
        this.processor.process(event);

        Mockito.verify(adapter).fetchAccessToken(redirectUri);
        Mockito.verify(this.manager.getAccessTokenPoolFactory()).passivateObject(accessTokenId, adapter);
        Mockito.verify(event.getMessage()).setInvocationProperty("OAuthAccessTokenId", accessTokenId);

    }

    @Test
    public void adapterWitouthUrl() throws Exception
    {
        final String accessTokenUrl = "accessTokenUrl";
        final String redirectUri = "redirectUri";
        final String accessTokenId = "accessTokenId";

        this.processor.setRedirectUri(redirectUri);
        this.processor.setAccessTokenUrl(accessTokenUrl);

        OAuth2Adapter adapter = Mockito.mock(OAuth2Adapter.class);

        Mockito.when(this.manager.createAdapter(verifier)).thenReturn(adapter);
        Mockito.when(adapter.getAccessTokenId()).thenReturn(accessTokenId);

        MuleEvent event = this.getEvent();
        this.processor.process(event);

        Mockito.verify(adapter).fetchAccessToken(redirectUri);
        Mockito.verify(this.manager.getAccessTokenPoolFactory()).passivateObject(accessTokenId, adapter);
        Mockito.verify(event.getMessage()).setInvocationProperty("OAuthAccessTokenId", accessTokenId);

    }

    private MuleEvent getEvent()
    {
        MuleEvent event = Mockito.mock(MuleEvent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(event.getMessage().getInvocationProperty("_oauthVerifier")).thenReturn(verifier);

        return event;
    }

}
