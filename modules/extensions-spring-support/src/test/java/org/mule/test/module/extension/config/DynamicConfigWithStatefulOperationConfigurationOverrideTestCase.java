/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import static org.junit.Assert.fail;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
// TODO MULE-19352 migrate this test to InvalidExtensionConfigTestCase
public class DynamicConfigWithStatefulOperationConfigurationOverrideTestCase extends AbstractExtensionFunctionalTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    // TODO MULE-10061 - Review once the MuleContext lifecycle is clearly defined
    expectedException.expect(InitialisationException.class);
    additionalExceptionAssertions(expectedException);
  }

  @Override
  protected String getConfigFile() {
    return "validation/dynamic-stateful-override-config.xml";
  }

  protected void additionalExceptionAssertions(ExpectedException expectedException) {
    expectedException.expectMessage("Component 'implicit:get-enriched-name' at implicitConfig/processors/0 uses a dynamic "
        + "configuration and defines configuration override parameter 'optionalWithDefault' which is "
        + "assigned on initialization. That combination is not supported. Please use a non dynamic "
        + "configuration or don't set the parameter.");
  }

  @Test
  public void failedValidation() {
    fail("Config should have failed to parse");
  }
}
