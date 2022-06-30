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

import static java.lang.Boolean.parseBoolean;
import static java.util.Arrays.asList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.size.SmallTest;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;

import org.junit.Before;
import org.junit.runners.Parameterized;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

import java.util.Collection;

@SmallTest
@Issue("W-11117613")
@Feature(ERROR_HANDLING)
@Story(GLOBAL_ERROR_HANDLER)
@RunWith(Parameterized.class)
public class CoreComponentBuildingDefinitionProviderTestCase extends AbstractMuleTestCase {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Rule
  public SystemProperty reuseGlobalErrorHandlerRule;

  private CoreComponentBuildingDefinitionProvider provider;

  private final boolean reuseGlobalErrorHandler;

  public CoreComponentBuildingDefinitionProviderTestCase(String reuseGlobalErrorHandler) {
    this.reuseGlobalErrorHandler = parseBoolean(reuseGlobalErrorHandler);
    reuseGlobalErrorHandlerRule = new SystemProperty(REUSE_GLOBAL_ERROR_HANDLER_PROPERTY, reuseGlobalErrorHandler);
  }

  @Parameterized.Parameters(name = "enable system property: {0}")
  public static Collection params() {
    return asList("true", "false");
  }

  @Before
  public void setUp() throws Exception {
    provider = new CoreComponentBuildingDefinitionProvider();
  }

  @Test
  public void isPrototype() {
    ComponentBuildingDefinition buildingDefinition = provider.getErrorHandlerBuilder().build();

    assertThat(buildingDefinition.isPrototype(), is(not(reuseGlobalErrorHandler)));
  }
}
