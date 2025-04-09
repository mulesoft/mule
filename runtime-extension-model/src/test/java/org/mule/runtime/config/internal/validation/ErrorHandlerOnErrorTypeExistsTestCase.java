/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static java.util.Optional.ofNullable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.config.internal.validation.test.AbstractCoreValidationTestCase;

import java.util.Optional;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Features({@Feature(ERROR_HANDLING), @Feature(MULE_DSL)})
@Story(DSL_VALIDATION_STORY)
public class ErrorHandlerOnErrorTypeExistsTestCase extends AbstractCoreValidationTestCase {

  FeatureFlaggingService featureFlaggingService = mock(FeatureFlaggingService.class);
  boolean ignoreParams = true;

  @Override
  protected Validation getValidation() {
    return new ErrorHandlerOnErrorTypeExists(ofNullable(featureFlaggingService), ignoreParams);
  }

  @Test
  @Issue("W-12769196")
  @Description("Without type, it doesn't go through validation process due to the criteria of ErrorHandlerOnErrorTypeExists#applicable. "
      +
      "This test was added to check if the applicable method is throwing NPE without the fix.")
  public void errorHandlerWithoutTypeDoesNotFailValidation() {

    final Optional<ValidationResultItem> msg =
        runValidation("ErrorHandlerOnErrorTypeExistsTestCase#errorHandlerWithoutTypeDoesNotFailValidation",
                      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
                          "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                          "      xsi:schemaLocation=\"\n" +
                          "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n"
                          +
                          "\n" +
                          "    <flow name=\"flowA\">\n" +
                          "        <error-handler>\n" +
                          "            <on-error-propagate>\n" +
                          "            <logger level=\"ERROR\" message=\"Check Failed.\" />\n" +
                          "            </on-error-propagate>\n" +
                          "        </error-handler>\n" +
                          "    </flow>\n" +
                          "\n" +
                          "</mule>")
            .stream().findFirst();

    assertThat(msg.isPresent(), equalTo(false));
  }
}
