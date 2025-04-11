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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.config.internal.validation.test.AbstractCoreValidationTestCase;

import java.util.Optional;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Features({@Feature(ERROR_HANDLING), @Feature(MULE_DSL)})
@Story(DSL_VALIDATION_STORY)
public class ErrorHandlerOnErrorTypeNonPropertyValueTestCase extends AbstractCoreValidationTestCase {

  @Override
  protected Validation getValidation() {
    return new ErrorHandlerOnErrorTypeNonPropertyValue();
  }

  @Test
  @Issue("W-16083021")
  public void onErrorRefDoesntCauseNpe() {
    final Optional<ValidationResultItem> msg =
        runValidation("ErrorHandlerOnErrorTypeNonPropertyValueTestCase#onErrorRefDoesntCauseNpe",
                      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<mule xmlns:http=\"http://www.mulesoft.org/schema/mule/http\"\n" +
                          "      xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
                          "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                          "      xsi:schemaLocation=\"\n" +
                          "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n"
                          +
                          "\n" +
                          "    <on-error-continue name=\"sharedErrorHandler\">\n" +
                          "        <logger/>\n" +
                          "    </on-error-continue>\n" +
                          "\n" +
                          "    <flow name=\"withSharedHandler\">\n" +
                          "        <error-handler >\n" +
                          "            <on-error ref=\"sharedErrorHandler\"/>\n" +
                          "        </error-handler>\n" +
                          "    </flow>\n" +
                          "</mule>")
            .stream().findFirst();

    assertThat(msg.isPresent(), equalTo(false));
  }
}
