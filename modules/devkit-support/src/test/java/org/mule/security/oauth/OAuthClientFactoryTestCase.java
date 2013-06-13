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

    private OAuth2Manager<OAuth2Adapter> manager;

    private TestClientFactory factory;

    @Mock
    private ObjectStore<OAuthState> objectStore;

    @Mock
    private MuleContext muleContext;

    private static final String consumerKey = "consumerKey";
    private static final String consumerSecret = "consumerSecret";

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
        Mockito.when(this.objectStore.contains("key")).thenReturn(false);
        this.factory.makeObject("key");
    }

    @Test
    public void makeObject() throws Exception
    {
        OAuthState state = this.registerState();

        TestOAuth2Adapter connector = (TestOAuth2Adapter) this.factory.makeObject("key");

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
    }

    @Test(expected = IllegalArgumentException.class)
    public void destroyWithIllegalKeyType() throws Exception
    {
        this.factory.destroyObject(new Object(), new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void destroyWithIllegalValueType() throws Exception
    {
        this.factory.destroyObject("key", new Object());
    }

    @Test
    public void destroy() throws Exception
    {
        TestOAuth2Adapter adapter = new TestOAuth2Adapter(this.manager);
        this.factory.destroyObject("key", adapter);
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
        this.factory.validateObject("key", new Object());
    }

    @Test
    public void validateObject() throws Exception
    {
        OAuthState state = this.registerState();
        TestOAuth2Adapter adapter = new TestOAuth2Adapter(this.manager);
        adapter.setAccessToken(state.getAccessToken());
        adapter.setRefreshToken(state.getRefreshToken());

        Assert.assertTrue(this.factory.validateObject("key", adapter));
    }

    @Test
    public void validateObjectWithNoAccessToken() throws Exception
    {
        this.registerState();
        TestOAuth2Adapter adapter = new TestOAuth2Adapter(this.manager);
        adapter.setAccessToken(null);

        Assert.assertFalse(this.factory.validateObject("key", adapter));
    }

    @Test
    public void validateObjectWithWrongAccessToken() throws Exception
    {
        this.registerState();
        TestOAuth2Adapter adapter = new TestOAuth2Adapter(this.manager);
        adapter.setAccessToken("I'm wrong");

        Assert.assertFalse(this.factory.validateObject("key", adapter));
    }

    @Test
    public void validateObjectWithNoRefreshTokenButStateThatDoes() throws Exception
    {
        OAuthState state = this.registerState();
        TestOAuth2Adapter adapter = new TestOAuth2Adapter(this.manager);
        adapter.setAccessToken(state.getAccessToken());
        adapter.setRefreshToken(null);

        Assert.assertFalse(this.factory.validateObject("key", adapter));
    }

    @Test
    public void validateObjectWithNoRefreshTokenButStateThatDoesntEither() throws Exception
    {
        OAuthState state = this.registerState();
        TestOAuth2Adapter adapter = new TestOAuth2Adapter(this.manager);
        adapter.setAccessToken(state.getAccessToken());
        adapter.setRefreshToken(null);
        state.setRefreshToken(null);

        Assert.assertTrue(this.factory.validateObject("key", adapter));
    }

    @Test
    public void validateObjectWithWrongRefresh() throws Exception
    {
        OAuthState state = this.registerState();
        TestOAuth2Adapter adapter = new TestOAuth2Adapter(this.manager);
        adapter.setAccessToken(state.getAccessToken());
        adapter.setRefreshToken("I'm wrong");

        Assert.assertFalse(this.factory.validateObject("key", adapter));
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
        this.factory.passivateObject("key", new Object());
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

        this.factory.passivateObject("key", adapter);

        Assert.assertEquals(adapter.getAccessToken(), state.getAccessToken());
        Assert.assertEquals(adapter.getAccessTokenUrl(), state.getAccessTokenUrl());
        Assert.assertEquals(adapter.getAuthorizationUrl(), state.getAuthorizationUrl());
        Assert.assertEquals(adapter.getRefreshToken(), state.getRefreshToken());

        Mockito.verify(this.objectStore).remove("key");
        Mockito.verify(this.factory).setCustomStateProperties(adapter, state);
        Mockito.verify(this.objectStore).store("key", state);
    }

    @Test
    public void passivateUnexisting() throws Exception
    {
        Mockito.when(this.objectStore.contains("key")).thenReturn(false);
        final TestOAuth2Adapter adapter = new TestOAuth2Adapter(this.manager);

        adapter.setAccessToken("new access token");
        adapter.setAccessTokenUrl("access token url");
        adapter.setAuthorizationUrl("authorization url");
        adapter.setRefreshToken("refresh token");

        this.factory.passivateObject("key", adapter);

        Mockito.verify(this.objectStore, Mockito.never()).retrieve("key");
        Mockito.verify(this.objectStore, Mockito.never()).remove("key");
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
        }).when(this.objectStore).store(Mockito.eq("key"), Mockito.any(OAuthState.class));
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

        Mockito.when(this.objectStore.contains("key")).thenReturn(true);
        Mockito.when(this.objectStore.retrieve("key")).thenReturn(state);
        return state;
    }

    private class TestClientFactory extends BaseOAuthClientFactory
    {

        public TestClientFactory(OAuth2Manager<OAuth2Adapter> manager, ObjectStore<OAuthState> objectStore)
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
}
