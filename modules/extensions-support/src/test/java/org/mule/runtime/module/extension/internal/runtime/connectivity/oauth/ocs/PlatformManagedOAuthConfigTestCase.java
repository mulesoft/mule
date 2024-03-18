/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_API_VERSION;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_CLIENT_ID;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_CLIENT_SECRET;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_ORG_ID;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_PLATFORM_AUTH_URL;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_SERVICE_URL;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_PLATFORM_AUTH_PATH;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_PLATFORM_AUTH_DEFAULT_PATH;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class PlatformManagedOAuthConfigTestCase extends AbstractMuleTestCase {

  private static final String CLIENT_ID = "client_id";
  private static final String SECRET_ID = "secret_id";
  private static final String ORG_ID = "org_id";
  private static final String SERVICE_URL = "service_url";
  private static final String PLATFORM_AUTH_URL = "http://localhost/accounts";
  private static final String PLATFORM_AUTH_PATH = "/token";
  private static final String CUSTOM_OCS_API_VERSION = "v80";
  private ConfigurationProperties configurationProperties;

  @Before
  public void before() {
    configurationProperties = mock(ConfigurationProperties.class);
    when(configurationProperties.resolveStringProperty(OCS_CLIENT_SECRET)).thenReturn(of(SECRET_ID));
    when(configurationProperties.resolveStringProperty(OCS_CLIENT_ID)).thenReturn(of(CLIENT_ID));
    when(configurationProperties.resolveStringProperty(OCS_ORG_ID)).thenReturn(of(ORG_ID));
    when(configurationProperties.resolveStringProperty(OCS_SERVICE_URL)).thenReturn(of(SERVICE_URL));
    when(configurationProperties.resolveStringProperty(OCS_PLATFORM_AUTH_URL)).thenReturn(of(PLATFORM_AUTH_URL));
    when(configurationProperties.resolveStringProperty(OCS_API_VERSION)).thenReturn(empty());
  }


  @Test
  public void getNullApiVersion() {
    PlatformManagedOAuthConfig config = PlatformManagedOAuthConfig.from("", "", null, null, null, null, configurationProperties);
    assertThat(config.getApiVersion(), is(nullValue()));
  }


  @Test
  public void getCustomApiVersion() {
    when(configurationProperties.resolveStringProperty(OCS_API_VERSION)).thenReturn(of(CUSTOM_OCS_API_VERSION));
    PlatformManagedOAuthConfig config = PlatformManagedOAuthConfig.from("", "", null, null, null, null, configurationProperties);
    assertThat(config.getApiVersion(), equalTo(CUSTOM_OCS_API_VERSION));
  }

  @Test
  public void getPlatformAuthUrl() {
    PlatformManagedOAuthConfig config = PlatformManagedOAuthConfig.from("", "", null, null, null, null, configurationProperties);
    assertThat(config.getPlatformAuthUrl(), equalTo(PLATFORM_AUTH_URL + OCS_PLATFORM_AUTH_DEFAULT_PATH));
  }

  @Test
  public void getPlatformAuthUrlWithPath() {
    when(configurationProperties.resolveStringProperty(OCS_PLATFORM_AUTH_PATH)).thenReturn(of(PLATFORM_AUTH_PATH));
    PlatformManagedOAuthConfig config = PlatformManagedOAuthConfig.from("", "", null, null, null, null, configurationProperties);
    assertThat(config.getPlatformAuthUrl(), equalTo(PLATFORM_AUTH_URL + PLATFORM_AUTH_PATH));
  }

}
