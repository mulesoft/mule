/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.config.internal.validation.test.AbstractCoreValidationTestCase;

import java.util.Optional;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
public class NoExpressionsInNoExpressionsSupportedParamsTestCase extends AbstractCoreValidationTestCase {

  @Override
  protected Validation getValidation() {
    return new NoExpressionsInNoExpressionsSupportedParams();
  }

  @Test
  public void invalidParameterWithExpression() {
    final Optional<ValidationResultItem> msg =
        runValidation("NoExpressionsInNoExpressionsSupportedParamsTestCase#invalidParameterWithExpression",
                      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
                          "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                          "      xsi:schemaLocation=\"\n" +
                          "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n"
                          +
                          "\n" +
                          "    <flow name=\"flow\">\n" +
                          "        <logger level=\"#['WARN']\"/>\n" +
                          "    </flow>\n" +
                          "\n" +
                          "</mule>")
            .stream().findFirst();

    assertThat(msg.get().getMessage(),
               containsString("An expression value was given for parameter 'level' but it doesn't support expressions"));
  }

  @Test
  public void invalidExpressionParameterInsideGroup() {
    final Optional<ValidationResultItem> msg =
        runValidation("NoExpressionsInNoExpressionsSupportedParamsTestCase#invalidExpressionParameterInsideGroup",
                      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
                          "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                          "      xsi:schemaLocation=\"\n" +
                          "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n"
                          +
                          "\n" +
                          "    <flow name=\"flow\">\n" +
                          "        <scheduler>\n" +
                          "            <scheduling-strategy>\n" +
                          "                <fixed-frequency frequency=\"#[frequency]\"/>\n" +
                          "            </scheduling-strategy>\n" +
                          "        </scheduler>\n" +
                          "        <logger/>\n" +
                          "    </flow>\n" +
                          "\n" +
                          "</mule>")
            .stream().findFirst();

    assertThat(msg.get().getMessage(),
               containsString("An expression value was given for parameter 'frequency' but it doesn't support expressions"));
  }

}
