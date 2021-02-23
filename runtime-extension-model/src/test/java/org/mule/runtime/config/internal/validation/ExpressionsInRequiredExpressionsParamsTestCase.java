/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.mule.runtime.ast.api.validation.Validation;

import java.util.Optional;

public class ExpressionsInRequiredExpressionsParamsTestCase extends AbstractCoreValidationTestCase {

  @Override
  protected Validation getValidation() {
    return new ExpressionsInRequiredExpressionsParams();
  }

  @Test
  public void requiredExpression() {
    final Optional<String> msg = runValidation("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
        "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "      xsi:schemaLocation=\"\n" +
        "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n" +
        "\n" +
        "<configuration correlationIdGeneratorExpression=\"(uuid() splitBy('-'))[2] ++ '*doge'\"/>\n" +
        "</mule>");

    assertThat(msg.isPresent(), is(true));
    assertThat(msg.get(),
               containsString("A static value was given for parameter 'correlationIdGeneratorExpression' but it requires a expression"));
  }
}
