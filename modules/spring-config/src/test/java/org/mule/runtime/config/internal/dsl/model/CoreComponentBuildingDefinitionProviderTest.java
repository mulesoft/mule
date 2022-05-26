/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static org.mule.runtime.api.util.MuleSystemProperties.REUSE_GLOBAL_ERROR_HANDLER_PROPERTY;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.GLOBAL_ERROR_HANDLER;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;

import org.junit.Before;
import org.junit.Test;
import org.mule.tck.size.SmallTest;

@SmallTest
@Feature(ERROR_HANDLING)
@Story(GLOBAL_ERROR_HANDLER)
public class CoreComponentBuildingDefinitionProviderTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  private CoreComponentBuildingDefinitionProvider provider;

  @Before
  public void setUp() throws Exception {
    provider = new CoreComponentBuildingDefinitionProvider();
  }

  @Test
  public void isPrototypeWhenSystemPropertyIsDisabled() {
    String originalValue = System.clearProperty(REUSE_GLOBAL_ERROR_HANDLER_PROPERTY);
    System.setProperty(REUSE_GLOBAL_ERROR_HANDLER_PROPERTY, "false");

    try {
      ComponentBuildingDefinition buildingDefinition = provider.getErrorHandlerBuilder().build();

      assertThat(buildingDefinition.isPrototype(), is(true));
    } finally {
      restoreSystemProperty(REUSE_GLOBAL_ERROR_HANDLER_PROPERTY, originalValue);
    }
  }

  @Test
  public void isSingletonWhenSystemPropertyIsEnabled() {
    String originalValue = System.clearProperty(REUSE_GLOBAL_ERROR_HANDLER_PROPERTY);
    System.setProperty(REUSE_GLOBAL_ERROR_HANDLER_PROPERTY, "true");

    try {
      ComponentBuildingDefinition buildingDefinition = provider.getErrorHandlerBuilder().build();

      assertThat(buildingDefinition.isPrototype(), is(false));
    } finally {
      restoreSystemProperty(REUSE_GLOBAL_ERROR_HANDLER_PROPERTY, originalValue);
    }
  }

  private void restoreSystemProperty(String name, String originalValue) {
    if (originalValue == null) {
      System.clearProperty(name);
    } else {
      System.setProperty(name, originalValue);
    }
  }
}
