/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth;

import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_API_VERSION;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_CLIENT_ID;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_CLIENT_SECRET;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_ORG_ID;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_PLATFORM_AUTH_URL;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_SERVICE_URL;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode.AuthorizationCodeOAuthHandler;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs.PlatformManagedOAuthConfig;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OAuthHandlerTestCase extends AbstractMuleContextTestCase {

  private static final String SOME_KEY = "1874947571-1840879217-380895431-1745289126";
  private static final String OTHER_KEY = "1874947571-1840879217-123123123-1745289126";

  private static final List<String> OBJECT_STORE_ENTRIES = Arrays.asList(SOME_KEY, OTHER_KEY);

  @InjectMocks
  AuthorizationCodeOAuthHandler oauthHandler = new AuthorizationCodeOAuthHandler();

  @Mock
  private ObjectStoreManager storeManager;

  @Mock
  private ObjectStore<MetadataCache> objectStore;

  private ConfigurationProperties configurationProperties;
  private static final String CLIENT_ID = "client_id";
  private static final String SECRET_ID = "secret_id";
  private static final String ORG_ID = "org_id";
  private static final String SERVICE_URL = "service_url";
  private static final String PLATFORM_AUTH_URL = "http://localhost/accounts";

  @Test
  @Issue("W-11410770")
  @Description("Verify that ObjectStore is created, if none exists by calling getOrCreateObjectStore")
  public void verifyThatObjectStoreisAlwaysCreated() throws InitialisationException, ObjectStoreException {
    PlatformManagedOAuthConfig config = PlatformManagedOAuthConfig.from("", "", null, null, null, null, configurationProperties);

    oauthHandler.initialise();

    ObjectStore objectStore = oauthHandler.getObjectStoreLocator().apply(config);
    assertThat(objectStore, is(notNullValue()));
    assertThat(objectStore.allKeys(), is(OBJECT_STORE_ENTRIES));
    verify(storeManager, times(1)).getOrCreateObjectStore(anyString(), any(ObjectStoreSettings.class));
  }

  @Rule
  public TemporaryFolder tempWorkDir = new TemporaryFolder();

  @Before
  public void setup() throws ObjectStoreException {

    when(storeManager.getOrCreateObjectStore(anyString(), any()))
        .thenReturn(objectStore);
    when(objectStore.allKeys()).thenReturn(Arrays.asList(SOME_KEY, OTHER_KEY));

    configurationProperties = mock(ConfigurationProperties.class);
    when(configurationProperties.resolveStringProperty(OCS_CLIENT_SECRET)).thenReturn(of(SECRET_ID));
    when(configurationProperties.resolveStringProperty(OCS_CLIENT_ID)).thenReturn(of(CLIENT_ID));
    when(configurationProperties.resolveStringProperty(OCS_ORG_ID)).thenReturn(of(ORG_ID));
    when(configurationProperties.resolveStringProperty(OCS_SERVICE_URL)).thenReturn(of(SERVICE_URL));
    when(configurationProperties.resolveStringProperty(OCS_PLATFORM_AUTH_URL)).thenReturn(of(PLATFORM_AUTH_URL));
    when(configurationProperties.resolveStringProperty(OCS_API_VERSION)).thenReturn(empty());
    when(configurationProperties.resolveStringProperty(OCS_API_VERSION)).thenReturn(empty());
  }

  @Override
  protected void doTearDown() throws Exception {
    oauthHandler.stop();
  }

}
