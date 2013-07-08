/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth;

import org.mule.api.MuleContext;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.common.security.oauth.OAuthState;
import org.mule.tck.size.SmallTest;

import java.io.Serializable;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class OAuthClientFactoryTestCase
{

    private static final String KEY = "key";
    private static final String consumerKey = "consumerKey";
    private static final String consumerSecret = "consumerSecret";

    private OAuth2Manager<OAuth2Adapter> manager;

    private TestClientFactory factory;

    @Mock
    private ObjectStore<Serializable> objectStore;

    @Mock
    private MuleContext muleContext;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp()
    {
        this.manager = (OAuth2Manager<OAuth2Adapter>) Mockito.mock(OAuth2Manager.class,
            Mockito.RETURNS_DEEP_STUBS);

        Mockito.when(this.manager.getDefaultUnauthorizedConnector().getConsumerKey()).thenReturn(consumerKey);
        Mockito.when(this.manager.getDefaultUnauthorizedConnector().getConsumerSecret()).thenReturn(
            consumerSecret);
        Mockito.when(this.manager.getMuleContext()).thenReturn(this.muleContext);

        this.factory = Mockito.spy(new TestClientFactory(this.manager, this.objectStore));
    }

    @Test(expected = IllegalArgumentException.class)
    public void makeObjectWithWrongKeyType() throws Exception
    {
        this.factory.makeObject(new Object());
    }

    @Test(expected = RuntimeException.class)
    public void makeObjectWithNotExistentKey() throws Exception
    {
        Mockito.when(this.objectStore.contains(KEY)).thenReturn(false);
        this.factory.makeObject(KEY);
    }

    @Test
    public void makeObject() throws Exception
    {
        OAuthState state = this.registerState();

        TestOAuth2Adapter connector = (TestOAuth2Adapter) this.factory.makeObject(KEY);

        Assert.assertSame(connector.getManager(), this.manager);
        Mockito.verify(this.factory).setCustomAdapterProperties(connector, state);
        Assert.assertTrue(connector.wasPostAuthCalled());
        Assert.assertTrue(connector.wasStarted());
        Assert.assertTrue(connector.wasInitialised());
        Assert.assertSame(connector.getMuleContext(), this.muleContext);

        Assert.assertEquals(connector.getConsumerKey(), consumerKey);
        Assert.assertEquals(connector.getConsumerSecret(), consumerSecret);
        Assert.assertEquals(connector.getAccessToken(), state.getAccessToken());
        Assert.assertEquals(connector.getAccessTokenUrl(), state.getAccessTokenUrl());
        Assert.assertEquals(connector.getAuthorizationUrl(), state.getAuthorizationUrl());
        Assert.assertEquals(connector.getRefreshToken(), state.getRefreshToken());

        Mockito.verify(this.objectStore, Mockito.never()).remove(KEY);
        Mockito.verify(this.objectStore, Mockito.never()).store(Mockito.eq(KEY),
            Mockito.any(OAuthState.class));
    }

    @Test
    public void makeObjectWithOldState() throws Exception
    {
        final OldOAuthState state = this.registerOldState();

        Mockito.doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                OAuthState newState = (OAuthState) invocation.getArguments()[1];
                Assert.assertEquals(newState.getAccessToken(), state.getAccessToken());
                Assert.assertEquals(newState.getAccessTokenUrl(), state.getAccessTokenUrl());
                Assert.assertEquals(newState.getAuthorizationUrl(), state.getAuthorizationUrl());
                Assert.assertEquals(newState.getRefreshToken(), state.getRefreshToken());
                Assert.assertEquals(newState.getCustomProperty("custom1"), state.getCustom1());
                Assert.assertEquals(newState.getCustomProperty("custom2"), state.getCustom2());

                state.setChecked(true);
                return null;
            }
        })
            .when(this.factory)
            .setCustomAdapterProperties(Mockito.any(TestOAuth2Adapter.class), Mockito.any(OAuthState.class));

        TestOAuth2Adapter connector = (TestOAuth2Adapter) this.factory.makeObject(KEY);

        Assert.assertTrue(state.isChecked());

        Assert.assertSame(connector.getManager(), this.manager);
        Assert.assertTrue(connector.wasPostAuthCalled());
        Assert.assertTrue(connector.wasStarted());
        Assert.assertTrue(connector.wasInitialised());
        Assert.assertSame(connector.getMuleContext(), this.muleContext);

        Assert.assertEquals(connector.getConsumerKey(), consumerKey);
        Assert.assertEquals(connector.getConsumerSecret(), consumerSecret);
        Assert.assertEquals(connector.getAccessToken(), state.getAccessToken());
        Assert.assertEquals(connector.getAccessTokenUrl(), state.getAccessTokenUrl());
        Assert.assertEquals(connector.getAuthorizationUrl(), state.getAuthorizationUrl());
        Assert.assertEquals(connector.getRefreshToken(), state.getRefreshToken());

        Mockito.verify(this.objectStore).remove(KEY);
        Mockito.verify(this.objectStore).store(Mockito.eq(KEY), Mockito.any(OAuthState.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void destroyWithIllegalKeyType() throws Exception
    {
        this.factory.destroyObject(new Object(), new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void destroyWithIllegalValueType() throws Exception
    {
        this.factory.destroyObject(KEY, new Object());
    }

    @Test
    public void destroy() throws Exception
    {
        TestOAuth2Adapter adapter = new TestOAuth2Adapter(this.manager);
        this.factory.destroyObject(KEY, adapter);
        Assert.assertTrue(adapter.wasStopped());
        Assert.assertTrue(adapter.wasDisposed());
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateObjectWithIllegalKey() throws Exception
    {
        this.factory.validateObject(new Object(), new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateObjectOfIllegalType() throws Exception
    {
        this.factory.validateObject(KEY, new Object());
    }

    @Test
    public void validateObject() throws Exception
    {
        OAuthState state = this.registerState();
        TestOAuth2Adapter adapter = new TestOAuth2Adapter(this.manager);
        adapter.setAccessToken(state.getAccessToken());
        adapter.setRefreshToken(state.getRefreshToken());

        Assert.assertTrue(this.factory.validateObject(KEY, adapter));
    }

    @Test
    public void validateObjectWithNoAccessToken() throws Exception
    {
        this.registerState();
        TestOAuth2Adapter adapter = new TestOAuth2Adapter(this.manager);
        adapter.setAccessToken(null);

        Assert.assertFalse(this.factory.validateObject(KEY, adapter));
    }

    @Test
    public void validateObjectWithWrongAccessToken() throws Exception
    {
        this.registerState();
        TestOAuth2Adapter adapter = new TestOAuth2Adapter(this.manager);
        adapter.setAccessToken("I'm wrong");

        Assert.assertFalse(this.factory.validateObject(KEY, adapter));
    }

    @Test
    public void validateObjectWithNoRefreshTokenButStateThatDoes() throws Exception
    {
        OAuthState state = this.registerState();
        TestOAuth2Adapter adapter = new TestOAuth2Adapter(this.manager);
        adapter.setAccessToken(state.getAccessToken());
        adapter.setRefreshToken(null);

        Assert.assertFalse(this.factory.validateObject(KEY, adapter));
    }

    @Test
    public void validateObjectWithNoRefreshTokenButStateThatDoesntEither() throws Exception
    {
        OAuthState state = this.registerState();
        TestOAuth2Adapter adapter = new TestOAuth2Adapter(this.manager);
        adapter.setAccessToken(state.getAccessToken());
        adapter.setRefreshToken(null);
        state.setRefreshToken(null);

        Assert.assertTrue(this.factory.validateObject(KEY, adapter));
    }

    @Test
    public void validateObjectWithWrongRefresh() throws Exception
    {
        OAuthState state = this.registerState();
        TestOAuth2Adapter adapter = new TestOAuth2Adapter(this.manager);
        adapter.setAccessToken(state.getAccessToken());
        adapter.setRefreshToken("I'm wrong");

        Assert.assertFalse(this.factory.validateObject(KEY, adapter));
    }

    @Test
    public void validateObjectWithNotExistingKey() throws Exception
    {
        Assert.assertFalse(this.factory.validateObject("fakeKey", new TestOAuth2Adapter(this.manager)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void passivateWithWrongKeyType() throws Exception
    {
        this.factory.passivateObject(new Object(), new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void passivateWithWrongValueType() throws Exception
    {
        this.factory.passivateObject(KEY, new Object());
    }

    @Test
    public void passivateExisting() throws Exception
    {
        OAuthState state = this.registerState();
        TestOAuth2Adapter adapter = new TestOAuth2Adapter(this.manager);

        adapter.setAccessToken("new access token");
        adapter.setAccessTokenUrl("access token url");
        adapter.setAuthorizationUrl("authorization url");
        adapter.setRefreshToken("refresh token");

        this.factory.passivateObject(KEY, adapter);

        Assert.assertEquals(adapter.getAccessToken(), state.getAccessToken());
        Assert.assertEquals(adapter.getAccessTokenUrl(), state.getAccessTokenUrl());
        Assert.assertEquals(adapter.getAuthorizationUrl(), state.getAuthorizationUrl());
        Assert.assertEquals(adapter.getRefreshToken(), state.getRefreshToken());

        Mockito.verify(this.objectStore).remove(KEY);
        Mockito.verify(this.factory).setCustomStateProperties(adapter, state);
        Mockito.verify(this.objectStore).store(KEY, state);
    }

    @Test
    public void passivateUnexisting() throws Exception
    {
        Mockito.when(this.objectStore.contains(KEY)).thenReturn(false);
        final TestOAuth2Adapter adapter = new TestOAuth2Adapter(this.manager);

        adapter.setAccessToken("new access token");
        adapter.setAccessTokenUrl("access token url");
        adapter.setAuthorizationUrl("authorization url");
        adapter.setRefreshToken("refresh token");

        this.factory.passivateObject(KEY, adapter);

        Mockito.verify(this.objectStore, Mockito.never()).retrieve(KEY);
        Mockito.verify(this.objectStore, Mockito.never()).remove(KEY);
        Mockito.doAnswer(new Answer<Void>()
        {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                OAuthState state = (OAuthState) invocation.getArguments()[1];
                Assert.assertEquals(adapter.getAccessToken(), state.getAccessToken());
                Assert.assertEquals(adapter.getAccessTokenUrl(), state.getAccessTokenUrl());
                Assert.assertEquals(adapter.getAuthorizationUrl(), state.getAuthorizationUrl());
                Assert.assertEquals(adapter.getRefreshToken(), state.getRefreshToken());

                return null;
            }
        }).when(this.objectStore).store(Mockito.eq(KEY), Mockito.any(OAuthState.class));
    }

    private OAuthState registerState() throws ObjectStoreException
    {
        OAuthState state = new OAuthState();
        state.setAccessToken("accessToken");
        state.setAccessTokenUrl("accessTokenUrl");
        state.setAuthorizationUrl("authorizationUrl");
        state.setCustomProperty("custom1", "custom1");
        state.setCustomProperty("custom2", "custom2");
        state.setRefreshToken("refreshToken");

        Mockito.when(this.objectStore.contains(KEY)).thenReturn(true);
        Mockito.when(this.objectStore.retrieve(KEY)).thenReturn(state);

        return state;
    }

    private OldOAuthState registerOldState() throws ObjectStoreException
    {
        OldOAuthState state = new OldOAuthState();
        state.setAccessToken("accessToken");
        state.setAccessTokenUrl("accessTokenUrl");
        state.setAuthorizationUrl("authorizationUrl");
        state.setRefreshToken("refreshToken");
        state.setCustom1("custom1");
        state.setCustom2("custom2");

        Mockito.when(this.objectStore.contains(KEY)).thenReturn(true);
        Mockito.when(this.objectStore.retrieve(KEY)).thenReturn(state);

        return state;
    }

    private class TestClientFactory extends BaseOAuthClientFactory
    {

        public TestClientFactory(OAuth2Manager<OAuth2Adapter> manager, ObjectStore<Serializable> objectStore)
        {
            super(manager, objectStore);
        }

        @Override
        protected Class<? extends OAuth2Adapter> getAdapterClass()
        {
            return TestOAuth2Adapter.class;
        }

        protected void setCustomAdapterProperties(OAuth2Adapter adapter,
                                                  org.mule.common.security.oauth.OAuthState state)
        {
        };

        @Override
        protected void setCustomStateProperties(OAuth2Adapter adapter, OAuthState state)
        {
        }
    }

    private class OldOAuthState implements Serializable
    {

        private static final long serialVersionUID = 8840821869504383401L;

        private String accessToken;
        private String authorizationUrl;
        private String accessTokenUrl;
        private String refreshToken;
        private String custom1;
        private String custom2;
        private boolean checked = false;

        public String getAccessToken()
        {
            return accessToken;
        }

        public void setAccessToken(String accessToken)
        {
            this.accessToken = accessToken;
        }

        public String getAuthorizationUrl()
        {
            return authorizationUrl;
        }

        public void setAuthorizationUrl(String authorizationUrl)
        {
            this.authorizationUrl = authorizationUrl;
        }

        public String getAccessTokenUrl()
        {
            return accessTokenUrl;
        }

        public void setAccessTokenUrl(String accessTokenUrl)
        {
            this.accessTokenUrl = accessTokenUrl;
        }

        public String getRefreshToken()
        {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken)
        {
            this.refreshToken = refreshToken;
        }

        public String getCustom1()
        {
            return custom1;
        }

        public void setCustom1(String custom1)
        {
            this.custom1 = custom1;
        }

        public String getCustom2()
        {
            return custom2;
        }

        public void setCustom2(String custom2)
        {
            this.custom2 = custom2;
        }

        public boolean isChecked()
        {
            return checked;
        }

        public void setChecked(boolean checked)
        {
            this.checked = checked;
        }
    }
}
