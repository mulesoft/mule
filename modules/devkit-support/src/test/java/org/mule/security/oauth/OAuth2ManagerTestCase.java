/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.devkit.capability.ModuleCapability;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.store.ObjectStore;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.common.security.oauth.exception.NotAuthorizedException;
import org.mule.security.oauth.util.HttpUtil;
import org.mule.security.oauth.util.OAuthResponseParser;
import org.mule.tck.size.SmallTest;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.net.URLDecoder;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/**
 * This class contains unit tests for
 * {@link org.mule.security.oauth.BaseOAuth2Manager}. It doesn't make sense to test
 * the methods about storing and retrieving authorization events here since it would
 * require to do deep mocking on the mule message transforming logic. Such a level of
 * mocking would greatly reduce the value of this test. Therefore, those methods are
 * tested in the integration test
 * {@link org.mule.test.integration.security.oauth2.OAuth2AuthorizationEventTestCase}
 */
@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OAuth2ManagerTestCase
{

    private static final String SCOPE = "<<myScope>>";
    private static final String ACCESS_TOKEN_ID = "accessTokenId";

    private TestOAuth2Manager manager;

    @Mock
    private ObjectStore<Serializable> accessTokenObjectStore = null;

    private MuleContext muleContext = null;

    @Mock
    private KeyedPoolableObjectFactory<String, OAuth2Adapter> objectFactory = null;

    @Mock(extraInterfaces = {Initialisable.class, Startable.class, Stoppable.class, Disposable.class,
        MuleContextAware.class})
    private OAuth2Adapter adapter;
    
    @Mock
    private GenericKeyedObjectPool<String, OAuth2Adapter> accessTokenPool;

    @Mock
    private HttpUtil httpUtil;

    @Mock
    private OAuthResponseParser oauthResponseParser;

    @Mock
    private Transformer transformer;

    @Mock
    private RefreshTokenManager refreshTokenManager;

    @Before
    public void setUp() throws Exception
    {
        this.muleContext = Mockito.mock(MuleContext.class, Mockito.RETURNS_DEEP_STUBS);
        when(
                this.muleContext.getRegistry().lookupObject(
                        Mockito.eq(MuleProperties.DEFAULT_USER_OBJECT_STORE_NAME))).thenReturn(
            this.accessTokenObjectStore);

        when(
                muleContext.getRegistry().lookupTransformer(Mockito.any(DataType.class),
                                                            Mockito.any(DataType.class))).thenReturn(this.transformer);

        this.manager = Mockito.spy(new TestOAuth2Manager(this.objectFactory, this.adapter));
        this.manager.setMuleContext(this.muleContext);
        this.manager.setHttpUtil(this.httpUtil);
        this.manager.setOauthResponseParser(this.oauthResponseParser);
        this.manager.setScope(SCOPE);
        this.manager.setRefreshTokenManager(this.refreshTokenManager);

        this.manager.initialise();
        this.manager.start();
    }

    @Test
    public void initialize() throws InitialisationException
    {
        assertSame(this.manager.getAccessTokenObjectStore(), this.accessTokenObjectStore);
        verify(this.manager).createPoolFactory(this.manager, this.accessTokenObjectStore);
        verify((Initialisable) this.adapter).initialise();
    }

    @Test
    public void start() throws MuleException
    {
        verify((Startable) this.adapter).start();
    }

    @Test
    public void stop() throws MuleException
    {
        this.manager.stop();
        verify((Stoppable) this.adapter).stop();
    }

    @Test
    public void dispose() throws MuleException
    {
        this.manager.dispose();
        verify((Disposable) this.adapter).dispose();
    }

    @Test
    public void createAdapter() throws Exception
    {
        final String verifier = "verifier";

        when(adapter.getAuthorizationUrl()).thenReturn("authorizationUrl");
        when(adapter.getAccessTokenUrl()).thenReturn("accessTokenUrl");
        when(adapter.getConsumerKey()).thenReturn("consumerKey");
        when(adapter.getConsumerSecret()).thenReturn("consumerSecret");

        OAuth2Adapter adapter = this.manager.createAdapter(verifier);

        assertSame(adapter, this.adapter);

        verify(adapter).setOauthVerifier(Mockito.eq(verifier));
        verify(adapter).setAuthorizationUrl(Mockito.eq(this.adapter.getAuthorizationUrl()));
        verify(adapter).setAccessTokenUrl(Mockito.eq(this.adapter.getAccessTokenUrl()));
        verify(adapter).setConsumerKey(Mockito.eq(this.adapter.getConsumerKey()));
        verify(adapter).setConsumerSecret(Mockito.eq(this.adapter.getConsumerSecret()));

        verify(this.manager).setCustomProperties(adapter);
        verify((MuleContextAware) adapter, Mockito.atLeastOnce()).setMuleContext(this.muleContext);
    }

    @Test
    public void buildAuthorizeUrl() throws Exception
    {
        final Map<String, String> extraParameters = new LinkedHashMap<String, String>();
        extraParameters.put("extra1", "extra1");
        extraParameters.put("extra2", "extra2");
        final String authorizationUrl = "authorizationUrl";
        final String redirectUri = "redirectUri";

        when(adapter.getAuthorizationUrl()).thenReturn(authorizationUrl);
        when(adapter.getConsumerKey()).thenReturn("consumerKey");

        Assert.assertEquals(
            URLDecoder.decode(this.manager.buildAuthorizeUrl(extraParameters, null, redirectUri), "UTF-8"),
            "authorizationUrl?response_type=code&client_id=consumerKey&scope=<<myScope>>&extra1=extra1&extra2=extra2&redirect_uri=redirectUri");

        Assert.assertEquals(
            URLDecoder.decode(this.manager.buildAuthorizeUrl(extraParameters, "custom", redirectUri), "UTF-8"),
            "custom?response_type=code&client_id=consumerKey&scope=<<myScope>>&extra1=extra1&extra2=extra2&redirect_uri=redirectUri");
    }

    @Test
    public void fetchAccessToken() throws Exception
    {
        final String accessTokenUrl = "accessTokenUrl";
        when(adapter.getAccessTokenUrl()).thenReturn(accessTokenUrl);

        final String oauthVerifier = "oauthVerifier";
        final String consumerKey = "consumerKey";
        final String consumerSecret = "consumerSecret";
        final String redirectUri = "redirectUri";
        final String response = "response";
        final String requestBody = "code=oauthVerifier&client_id=consumerKey&client_secret=consumerSecret&grant_type=authorization_code&redirect_uri=redirectUri";
        final Pattern accessTokenPattern = Pattern.compile(".");
        final Pattern expirationPattern = Pattern.compile(".");
        final Pattern refreshTokenPattern = Pattern.compile(".");
        final String accessToken = "accessToken";
        final String refreshToken = "refreshToken";
        final Date expiration = new Date();

        when(adapter.getOauthVerifier()).thenReturn(oauthVerifier);
        when(adapter.getConsumerKey()).thenReturn(consumerKey);
        when(adapter.getConsumerSecret()).thenReturn(consumerSecret);
        when(adapter.getAccessCodePattern()).thenReturn(accessTokenPattern);
        when(adapter.getRefreshTokenPattern()).thenReturn(refreshTokenPattern);
        when(adapter.getExpirationTimePattern()).thenReturn(expirationPattern);
        when(
                this.httpUtil.post(this.manager.getDefaultUnauthorizedConnector().getAccessTokenUrl(),
                                   requestBody)).thenReturn(response);
        when(this.oauthResponseParser.extractAccessCode(accessTokenPattern, response)).thenReturn(
            accessToken);
        when(this.oauthResponseParser.extractExpirationTime(expirationPattern, response)).thenReturn(
            expiration);
        when(this.oauthResponseParser.extractRefreshToken(refreshTokenPattern, response)).thenReturn(
            refreshToken);

        this.manager.fetchAccessToken(adapter, redirectUri);

        verify(this.httpUtil).post(
            this.manager.getDefaultUnauthorizedConnector().getAccessTokenUrl(), requestBody);
        verify(this.oauthResponseParser).extractAccessCode(accessTokenPattern, response);
        verify(this.adapter).setAccessToken(accessToken);
        verify(this.adapter).setExpiration(expiration);
        verify(this.adapter).setRefreshToken(refreshToken);

        verify(adapter).postAuth();
        verify(this.manager).fetchCallbackParameters(this.adapter, response);

    }

    @Test
    public void refreshAccessToken() throws Exception
    {
        final String accessTokenUrl = "accessTokenUrl";
        when(this.adapter.getAccessTokenUrl()).thenReturn(accessTokenUrl);

        final String oauthVerifier = "oauthVerifier";
        final String consumerKey = "consumerKey";
        final String consumerSecret = "consumerSecret";
        final String response = "response";
        final String requestBody = "grant_type=refresh_token&client_id=consumerKey&client_secret=consumerSecret&refresh_token=refreshToken";
        final Pattern accessTokenPattern = Pattern.compile(".");
        final Pattern expirationPattern = Pattern.compile(".");
        final Pattern refreshTokenPattern = Pattern.compile(".");
        final String accessToken = "accessToken";
        final String refreshToken = "refreshToken";
        final Date expiration = new Date();

        when(adapter.getOauthVerifier()).thenReturn(oauthVerifier);
        when(adapter.getConsumerKey()).thenReturn(consumerKey);
        when(adapter.getConsumerSecret()).thenReturn(consumerSecret);
        when(adapter.getAccessCodePattern()).thenReturn(accessTokenPattern);
        when(adapter.getRefreshTokenPattern()).thenReturn(refreshTokenPattern);
        when(adapter.getExpirationTimePattern()).thenReturn(expirationPattern);
        when(adapter.getRefreshToken()).thenReturn(refreshToken);
        when(
                this.httpUtil.post(this.manager.getDefaultUnauthorizedConnector().getAccessTokenUrl(),
                                   requestBody)).thenReturn(response);
        when(this.oauthResponseParser.extractAccessCode(accessTokenPattern, response)).thenReturn(
            accessToken);
        when(this.oauthResponseParser.extractExpirationTime(expirationPattern, response)).thenReturn(
            expiration);
        when(this.oauthResponseParser.extractRefreshToken(refreshTokenPattern, response)).thenReturn(
            refreshToken);

        this.manager.refreshAccessToken(adapter, accessToken);

        verify(this.adapter).setAccessToken(null);
        verify(this.httpUtil).post(
            this.manager.getDefaultUnauthorizedConnector().getAccessTokenUrl(), requestBody);
        verify(this.oauthResponseParser).extractAccessCode(accessTokenPattern, response);
        verify(this.adapter).setAccessToken(accessToken);
        verify(this.adapter).setExpiration(expiration);
        verify(this.adapter).setRefreshToken(refreshToken);
        verify(adapter).postAuth();
        verify(this.manager).fetchCallbackParameters(this.adapter, response);

    }

    @Test(expected = IllegalStateException.class)
    public void refreshWithoutToken() throws Exception
    {
        this.manager.refreshAccessToken(this.adapter, "myToken");
    }

    @Test
    public void hasBeenAuthorized() throws NotAuthorizedException
    {
        when(this.adapter.getAccessToken()).thenReturn(ACCESS_TOKEN_ID);
        this.manager.hasBeenAuthorized(this.adapter);
    }

    @Test(expected = NotAuthorizedException.class)
    public void hasBeenAuthorizedFailure() throws NotAuthorizedException
    {
        this.manager.hasBeenAuthorized(this.adapter);
    }

    @Test
    public void capabilities()
    {
        for (ModuleCapability capability : ModuleCapability.values())
        {
            if (capability == ModuleCapability.LIFECYCLE_CAPABLE
                || capability == ModuleCapability.OAUTH2_CAPABLE
                || capability == ModuleCapability.OAUTH_ACCESS_TOKEN_MANAGEMENT_CAPABLE)
            {
                Assert.assertTrue(this.manager.isCapableOf(capability));
            }
            else
            {
                Assert.assertFalse(this.manager.isCapableOf(capability));
            }
        }
    }

    @Test
    public void postAuthNoFailures() throws Exception
    {
        this.manager.postAuth(this.adapter, ACCESS_TOKEN_ID);
        verify(this.adapter).postAuth();
    }

    @Test
    public void postAuthWithRefreshableException() throws Exception
    {
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                reset(adapter);
                throw new FileNotFoundException();
            }
        }).when(this.adapter).postAuth();

        this.manager.postAuth(this.adapter, ACCESS_TOKEN_ID);
        verify(this.refreshTokenManager).refreshToken(adapter, ACCESS_TOKEN_ID);
    }

    @Test(expected = FileNotFoundException.class)
    public void postAuthWithRefreshableExceptionFailsAgain() throws Exception
    {
        doThrow(FileNotFoundException.class).when(this.adapter).postAuth();
        this.manager.postAuth(this.adapter, ACCESS_TOKEN_ID);
        verify(this.refreshTokenManager).refreshToken(adapter, ACCESS_TOKEN_ID);
    }

    @Test(expected = FileNotFoundException.class)
    public void postAuthWithRefreshableExceptionButNoAccessTokenId() throws Exception
    {
        doThrow(FileNotFoundException.class).when(this.adapter).postAuth();
        this.manager.postAuth(this.adapter, null);
        verify(this.refreshTokenManager, Mockito.never()).refreshToken(adapter, Mockito.anyString());
    }
    
    @Test(expected = RuntimeException.class)
    public void postAuthWithNonRefreshableException() throws Exception
    {
        doThrow(RuntimeException.class).when(this.adapter).postAuth();
        this.manager.postAuth(this.adapter, ACCESS_TOKEN_ID);
        verify(this.refreshTokenManager, Mockito.never()).refreshToken(adapter, Mockito.anyString());
    }

    @Test
    public void closeTokenPool() throws Exception
    {
        this.manager.setAccessTokenPool(this.accessTokenPool);
        this.manager.dispose();
        verify(this.accessTokenPool).close();
    }
}
