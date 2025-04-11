/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.util.Optional.empty;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.config.internal.validation.test.AbstractCoreValidationTestCase;

import java.util.Optional;

import org.junit.Test;

public class ExpressionsInRequiredExpressionsParamsTestCase extends AbstractCoreValidationTestCase {

  @Override
  protected Validation getValidation() {
    return new ExpressionsInRequiredExpressionsParams(empty(), false);
  }

  @Test
  public void requiredExpression() {
    final Optional<ValidationResultItem> msg = runValidation("ExpressionsInRequiredExpressionsParamsTestCase#requiredExpression",
                                                             "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                                 "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
                                                                 "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                                                                 +
                                                                 "      xsi:schemaLocation=\"\n" +
                                                                 "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n"
                                                                 +
                                                                 "    <configuration correlationIdGeneratorExpression=\"1, 2, 3\"/>\n"
                                                                 +
                                                                 "\n" +
                                                                 "</mule>")
        .stream().findFirst();

    assertThat(msg.isPresent(), is(true));
    assertThat(msg.get().getMessage(),
               containsString("A static value ('1, 2, 3') was given for parameter 'correlationIdGeneratorExpression' but it requires an expression"));
  }

  @Test
  public void requiredExpressionPresent() {
    final Optional<ValidationResultItem> msg =
        runValidation("ExpressionsInRequiredExpressionsParamsTestCase#requiredExpressionPresent",
                      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                          "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
                          "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                          "      xsi:schemaLocation=\"\n" +
                          "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n"
                          +
                          "    <flow name=\"flow\">\n" +
                          "        <foreach collection=\"#[1, 2, 3]\">\n" +
                          "            <logger message=\"hello\"/>\n" +
                          "        </foreach>\n" +
                          "    </flow>\n" +
                          "\n" +
                          "</mule>")
            .stream().findFirst();

    assertThat(msg.isPresent(), is(false));
  }
}
