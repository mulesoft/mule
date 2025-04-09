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
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
@Issue("W-10998630")
public class SetVariableVariableNameValidationTestCase extends AbstractCoreValidationTestCase {

  @Override
  protected Validation getValidation() {
    return new NoExpressionsInNoExpressionsSupportedParams();
  }

  @Test
  public void variableNameSupportsExpressionsSoNoValidationShouldArise() {
    final Optional<ValidationResultItem> msg =
        runValidation("SetVariableVariableNameValidationTestCase#variableNameSupportsExpressionsSoNoValidationShouldArise",
                      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\" xmlns:doc=\"http://www.mulesoft.org/schema/mule/documentation\"\n"
                          +
                          "   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                          "   xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n"
                          +
                          "   <flow name=\"expression-for-variablenameFlow\">\n" +
                          "       <set-variable value=\"myVariable\" variableName=\"targetName\"/>" +
                          "       <set-variable value=\"specialValue\" variableName=\"#[vars.targetName]\"/>\n" +
                          "   </flow>\n" +
                          "</mule>")
            .stream().findFirst();

    assertThat(msg.get().getMessage(),
               containsString("An expression value was given for parameter 'variableName' but it doesn't support expressions"));
  }
}
