/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.config.MuleRuntimeFeature.REUSE_GLOBAL_ERROR_HANDLER;

import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class CoreComponentBuildingDefinitionProviderTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  private CoreComponentBuildingDefinitionProvider provider;

  @Mock
  protected FeatureFlaggingService featureFlaggingService;

  @Before
  public void setUp() throws Exception {
    provider = new CoreComponentBuildingDefinitionProvider();
    provider.setFeatureFlaggingService(featureFlaggingService);
  }

  @Test
  public void isPrototypeWhenFeatureFlagIsDisabled() {
    when(featureFlaggingService.isEnabled(REUSE_GLOBAL_ERROR_HANDLER)).thenReturn(false);

    ComponentBuildingDefinition buildingDefinition = provider.getErrorHandlerBuilder().build();

    assertThat(buildingDefinition.isPrototype(), is(true));
  }

  @Test
  public void isSingletonWhenFeatureFlagIsEnabled() {
    when(featureFlaggingService.isEnabled(REUSE_GLOBAL_ERROR_HANDLER)).thenReturn(true);

    ComponentBuildingDefinition buildingDefinition = provider.getErrorHandlerBuilder().build();

    assertThat(buildingDefinition.isPrototype(), is(false));
  }
}
