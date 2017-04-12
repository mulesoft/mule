/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth.internal;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.oauth.api.AuthorizationCodeOAuthDancer;
import org.mule.runtime.oauth.api.ClientCredentialsOAuthDancer;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.runtime.oauth.api.builder.OAuthAuthorizationCodeDancerBuilder;
import org.mule.runtime.oauth.api.builder.OAuthClientCredentialsDancerBuilder;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.server.HttpServer;
import org.mule.services.oauth.internal.DefaultOAuthService;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Features;

@Features("OAuth Service")
public class OAuthContextTestCase extends AbstractMuleContextTestCase {

  private OAuthService service;
  private Map<String, ResourceOwnerOAuthContext> tokensStore;

  @Before
  public void before() throws ConnectionException, IOException, TimeoutException {
    final HttpService httpService = mock(HttpService.class);
    service = new DefaultOAuthService(httpService, mock(SchedulerService.class));

    tokensStore = new HashMap<>();
  }

  @Test
  public void clientCredentialsFirstGetContext() throws Exception {
    ClientCredentialsOAuthDancer clientCredentialsDancer =
        baseClientCredentialsDancerBuilder().tokenUrl(mock(HttpClient.class), "http://host/token").build();

    assertThat(tokensStore, not(hasKey(DEFAULT_RESOURCE_OWNER_ID)));
    ResourceOwnerOAuthContext contextFromDancer = clientCredentialsDancer.getContext();
    assertThat(contextFromDancer, not(nullValue()));
    assertThat(tokensStore, hasEntry(DEFAULT_RESOURCE_OWNER_ID, contextFromDancer));
  }

  @Test
  public void clientCredentialsFollowingGetContext() throws Exception {
    ClientCredentialsOAuthDancer clientCredentialsDancer =
        baseClientCredentialsDancerBuilder().tokenUrl(mock(HttpClient.class), "http://host/token").build();

    ResourceOwnerOAuthContext contextFromDancer = clientCredentialsDancer.getContext();

    assertThat(clientCredentialsDancer.getContext(), sameInstance(contextFromDancer));
    assertThat(tokensStore.entrySet(), hasSize(1));
  }

  @Test
  public void clientCredentialsInvalidateContext() throws Exception {
    ClientCredentialsOAuthDancer clientCredentialsDancer =
        baseClientCredentialsDancerBuilder().tokenUrl(mock(HttpClient.class), "http://host/token").build();

    assertThat(tokensStore, not(hasKey(DEFAULT_RESOURCE_OWNER_ID)));
    clientCredentialsDancer.getContext();
    assertThat(tokensStore, hasKey(DEFAULT_RESOURCE_OWNER_ID));

    clientCredentialsDancer.invalidateContext().get();
    assertThat(tokensStore, not(hasKey(DEFAULT_RESOURCE_OWNER_ID)));
  }

  @Test
  public void authCodeFirstGetContext() throws Exception {
    AuthorizationCodeOAuthDancer authCodeDancer =
        baseAuthCodeDancerbuilder().tokenUrl(mock(HttpClient.class), "http://host/token").build();

    assertThat(tokensStore, not(hasKey("user1")));
    ResourceOwnerOAuthContext contextFromDancer = authCodeDancer.getContextForResourceOwner("user1");
    assertThat(contextFromDancer, not(nullValue()));
    assertThat(tokensStore, hasEntry("user1", contextFromDancer));
  }

  @Test
  public void authCodeFollowingGetContext() throws Exception {
    AuthorizationCodeOAuthDancer authCodeDancer =
        baseAuthCodeDancerbuilder().tokenUrl(mock(HttpClient.class), "http://host/token").build();

    ResourceOwnerOAuthContext contextFromDancer = authCodeDancer.getContextForResourceOwner("user1");

    assertThat(authCodeDancer.getContextForResourceOwner("user1"), sameInstance(contextFromDancer));
    assertThat(tokensStore.entrySet(), hasSize(1));
  }

  @Test
  public void authCodeFollowingGetContextDiffernetUser() throws Exception {
    AuthorizationCodeOAuthDancer authCodeDancer =
        baseAuthCodeDancerbuilder().tokenUrl(mock(HttpClient.class), "http://host/token").build();

    ResourceOwnerOAuthContext contextFromDancer1 = authCodeDancer.getContextForResourceOwner("user1");
    ResourceOwnerOAuthContext contextFromDancer2 = authCodeDancer.getContextForResourceOwner("user2");

    assertThat(authCodeDancer.getContextForResourceOwner("user1"), sameInstance(contextFromDancer1));
    assertThat(authCodeDancer.getContextForResourceOwner("user2"), sameInstance(contextFromDancer2));
    assertThat(contextFromDancer1, not(sameInstance(contextFromDancer2)));
    assertThat(tokensStore.entrySet(), hasSize(2));
  }

  @Test
  public void authCodeInvalidateContext() throws Exception {
    AuthorizationCodeOAuthDancer authCodeDancer =
        baseAuthCodeDancerbuilder().tokenUrl(mock(HttpClient.class), "http://host/token").build();

    assertThat(tokensStore, not(hasKey("user1")));
    authCodeDancer.getContextForResourceOwner("user1");
    assertThat(tokensStore, hasKey("user1"));

    authCodeDancer.invalidateContext("user1").get();
    assertThat(tokensStore, not(hasKey("user1")));
  }

  @Test
  public void authCodeInvalidateContextDoesNotAffectOtherUsers() throws Exception {
    AuthorizationCodeOAuthDancer authCodeDancer =
        baseAuthCodeDancerbuilder().tokenUrl(mock(HttpClient.class), "http://host/token").build();

    authCodeDancer.getContextForResourceOwner("user1");
    authCodeDancer.getContextForResourceOwner("user2");

    assertThat(tokensStore.entrySet(), hasSize(2));
    assertThat(tokensStore, hasKey("user1"));
    assertThat(tokensStore, hasKey("user2"));

    authCodeDancer.invalidateContext("user1").get();
    assertThat(tokensStore, not(hasKey("user1")));
    assertThat(tokensStore, hasKey("user2"));
  }

  private OAuthClientCredentialsDancerBuilder baseClientCredentialsDancerBuilder() throws Exception {
    final OAuthClientCredentialsDancerBuilder builder =
        service.clientCredentialsGrantTypeDancerBuilder(muleContext.getRegistry().lookupObject(LockFactory.class),
                                                        tokensStore, mock(MuleExpressionLanguage.class));

    builder.clientCredentials("clientId", "clientSecret");
    builder.tokenUrl(mock(HttpClient.class), "http://host/token");
    return builder;
  }

  private OAuthAuthorizationCodeDancerBuilder baseAuthCodeDancerbuilder() throws Exception {
    final OAuthAuthorizationCodeDancerBuilder builder =
        service.authorizationCodeGrantTypeDancerBuilder(muleContext.getRegistry().lookupObject(LockFactory.class),
                                                        tokensStore, mock(MuleExpressionLanguage.class));

    builder.clientCredentials("clientId", "clientSecret");
    builder.tokenUrl(mock(HttpClient.class), "http://host/token");
    builder.authorizationUrl("http://host/auth");
    builder.localCallback(mock(HttpServer.class), "localCallback");
    return builder;
  }

}
