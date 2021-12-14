/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

public class ConfigurationCacheIdResolverTestCase {

  private static final String CONFIG_NAME = "Config name";
  private static final String CONFIG_CACHE_ID = "Config cache id";


  private ConfigurationCacheIdResolver configurationCacheIdResolver;

  private MuleContextWithRegistry muleContext = mock(MuleContextWithRegistry.class);
  private MuleRegistry muleRegistry = mock(MuleRegistry.class);
  private ConfigurationProvider configurationProvider =
      mock(ConfigurationProvider.class);

  @Before
  public void setup() {
    when(muleContext.getRegistry()).thenReturn(muleRegistry);
    when(muleRegistry.lookupByType(ArgumentMatchers.any())).thenReturn(null);
    when(configurationProvider.getName()).thenReturn(CONFIG_NAME);
    configurationCacheIdResolver = new ConfigurationCacheIdResolver(muleContext, configurationProvider);
  }

  @Test
  public void generateFallbackCacheIdDueToMissingService() {
    assertThat(configurationCacheIdResolver.getConfigurationCacheId(), is(CONFIG_NAME));
  }

  @Test
  public void generateCacheId() {
    when(muleRegistry.lookupByType(ArgumentMatchers.any())).thenReturn(null);
    assertThat(configurationCacheIdResolver.getConfigurationCacheId(), is(CONFIG_CACHE_ID));
  }

  @Test
  public void generateFallbackCacheIdDueToError() {
    assertThat(configurationCacheIdResolver.getConfigurationCacheId(), is(CONFIG_NAME));
  }



}
