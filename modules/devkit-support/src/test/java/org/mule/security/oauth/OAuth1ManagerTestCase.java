/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.signature.AuthorizationHeaderSigningStrategy;
import oauth.signpost.signature.HmacSha1MessageSigner;
import oauth.signpost.signature.OAuthMessageSigner;
import oauth.signpost.signature.SigningStrategy;

import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.common.security.oauth.exception.NotAuthorizedException;
import org.mule.security.oauth.callback.RestoreAccessTokenCallback;
import org.mule.tck.size.SmallTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OAuth1ManagerTestCase
{

    @Spy
    private TestOAuth1Manager manager = new TestOAuth1Manager();

    @Mock(extraInterfaces = {Initialisable.class, Startable.class, Stoppable.class, Disposable.class,
        MuleContextAware.class})
    private OAuth1Adapter adapter = null;

    @Mock
    private OAuthConsumer consumer;

    @Before
    public void setUp() throws Exception
    {
        Mockito.when(this.adapter.getConsumer()).thenReturn(this.consumer);
    }

    @Test
    public void restoreTokenWithCallback()
    {
        RestoreAccessTokenCallback callback = Mockito.mock(RestoreAccessTokenCallback.class);
        Mockito.when(this.adapter.getOauthRestoreAccessToken()).thenReturn(callback);
        final String accessToken = "accessToken";
        Mockito.when(callback.getAccessToken()).thenReturn(accessToken);

        Assert.assertTrue(this.manager.restoreAccessToken(this.adapter));

        Mockito.verify(callback).restoreAccessToken();
        Mockito.verify(adapter).setAccessToken(Mockito.eq(accessToken));
    }

    @Test
    public void restoreTokenWithoutCallback()
    {
        Assert.assertFalse(this.manager.restoreAccessToken(this.adapter));
    }

    @Test
    public void reset()
    {
        this.manager.reset(this.adapter);
        Mockito.verify(this.adapter).setAccessToken(null);
        Mockito.verify(this.adapter).setAccessTokenSecret(null);
        Mockito.verify(this.consumer).setTokenWithSecret(null, null);
    }

    @Test
    public void newConsumer()
    {
        Mockito.reset(this.adapter);

        final String consumerKey = "consumerKey";
        final String consumerSecret = "consumerSecret";
        final OAuthMessageSigner signer = new HmacSha1MessageSigner();
        final SigningStrategy signingStrategy = new AuthorizationHeaderSigningStrategy();

        Mockito.when(this.adapter.getConsumerKey()).thenReturn(consumerKey);
        Mockito.when(this.adapter.getConsumerSecret()).thenReturn(consumerSecret);
        Mockito.when(this.adapter.getSigningStrategy()).thenReturn(signingStrategy);
        Mockito.when(this.adapter.getMessageSigner()).thenReturn(signer);

        DefaultOAuthConsumer consumer = (DefaultOAuthConsumer) this.manager.getConsumer(this.adapter);

        Assert.assertEquals(consumer.getConsumerKey(), consumerKey);
        Assert.assertEquals(consumer.getConsumerSecret(), consumerSecret);

        Mockito.verify(this.adapter).setConsumer(consumer);
    }

    @Test(expected = NotAuthorizedException.class)
    public void notAuthorized() throws Exception
    {
        this.manager.hasBeenAuthorized(adapter);
        Mockito.verify(this.adapter, Mockito.times(2)).getAccessToken();
    }

    @Test
    public void authorizhedWithoutRestore() throws Exception
    {
        Mockito.when(this.adapter.getAccessToken()).thenReturn("accessToken");
        this.manager.hasBeenAuthorized(this.adapter);
    }

    @Test
    public void authorizeWithRestore() throws Exception
    {
        Mockito.when(this.adapter.getAccessToken()).thenReturn(null).thenReturn("accessToken");
        this.manager.hasBeenAuthorized(this.adapter);
    }
    
    private class TestOAuth1Manager extends BaseOAuth1Manager
    {

        private Logger logger = LoggerFactory.getLogger(TestOAuth1Manager.class);

        @Override
        protected Logger getLogger()
        {
            return this.logger;
        }

    }
}
