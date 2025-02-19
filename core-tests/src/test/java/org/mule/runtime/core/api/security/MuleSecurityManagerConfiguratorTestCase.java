/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;

import static java.util.Collections.singletonList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.security.DefaultMuleSecurityManager;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MuleSecurityManagerConfiguratorTestCase {

  @InjectMocks
  private MuleSecurityManagerConfigurator configurator;

  @Mock
  private MuleContext muleContext;

  @Mock
  private SecurityManager securityManager;

  @Mock
  private SecurityProvider provider;

  @Mock
  private EncryptionStrategy encryptionStrategy;

  @Before
  public void setUp() {
    when(muleContext.getSecurityManager()).thenReturn(securityManager);
  }

  @Test
  public void testSetProviders() throws Exception {
    List<SecurityProvider> providers = singletonList(provider);
    configurator.setProviders(providers);

    configurator.doGetObject();
    verify(securityManager).addProvider(provider);
  }

  @Test
  public void testSetEncryptionStrategies() throws Exception {
    List<EncryptionStrategy> strategies = singletonList(encryptionStrategy);
    configurator.setEncryptionStrategies(strategies);

    configurator.doGetObject();
    verify(securityManager).addEncryptionStrategy(encryptionStrategy);
  }

  @Test
  public void testDoGetObjectWithDefaultSecurityManager() throws Exception {
    configurator.setName(OBJECT_SECURITY_MANAGER);
    SecurityManager result = configurator.doGetObject();

    assertThat(result, is(securityManager));
  }

  @Test
  public void testDoGetObjectWithCustomSecurityManager() throws Exception {
    configurator.setName("custom");
    SecurityManager result = configurator.doGetObject();

    assertThat(result, is(not(securityManager)));
    assertThat(result, instanceOf(DefaultMuleSecurityManager.class));
  }
}
