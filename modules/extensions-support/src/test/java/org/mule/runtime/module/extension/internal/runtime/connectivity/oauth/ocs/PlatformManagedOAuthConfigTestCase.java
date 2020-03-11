/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs;

import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.*;

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

  private static final String CLIENT_ID = "a";
  private static final String SECRET_ID = "b";
  private static final String ORG_ID = "b";
  private static final String SERVICE_URL = "c";
  private static final String PLATFORM_URL = "http://localhost/accounts";
  private static final String PLATFORM_SUFFIX_DEFAULT = "/oauth2/token";
  private static final String PLATFORM_SUFFIX = "/token";
  private ConfigurationProperties configurationProperties;

  @Before
  public void before() {
    configurationProperties = mock(ConfigurationProperties.class);
    when(configurationProperties.resolveStringProperty(OCS_CLIENT_SECRET)).thenReturn(of(SECRET_ID));
    when(configurationProperties.resolveStringProperty(OCS_CLIENT_ID)).thenReturn(of(CLIENT_ID));
    when(configurationProperties.resolveStringProperty(OCS_ORG_ID)).thenReturn(of(ORG_ID));
    when(configurationProperties.resolveStringProperty(OCS_SERVICE_URL)).thenReturn(of(SERVICE_URL));
    when(configurationProperties.resolveStringProperty(OCS_PLATFORM_AUTH_URL)).thenReturn(of(PLATFORM_URL));
  }


  @Test
  public void getPlatformAuthUrl() {
    PlatformManagedOAuthConfig config = PlatformManagedOAuthConfig.from("", "", null, null, null, null, configurationProperties);
    assertThat(config.getPlatformAuthUrl(), equalTo(PLATFORM_URL + PLATFORM_SUFFIX_DEFAULT));
  }

  @Test
  public void getPlatformAuthUrlWithPlatformSuffix() {
    when(configurationProperties.resolveStringProperty(OCS_PLATFORM_AUTH_URL_SUFFIX)).thenReturn(of(PLATFORM_SUFFIX));
    PlatformManagedOAuthConfig config = PlatformManagedOAuthConfig.from("", "", null, null, null, null, configurationProperties);
    assertThat(config.getPlatformAuthUrl(), equalTo(PLATFORM_URL + PLATFORM_SUFFIX));
  }

}
