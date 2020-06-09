/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.extractExpression;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ARTIFACT_AST;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ParameterAst.PARAMETER_AST;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(ARTIFACT_AST)
@Story(PARAMETER_AST)
public class MulExtensionUtilsTestCase extends AbstractMuleTestCase {

  private static final String PAYLOAD_EXPRESSION = "#[payload]";
  private static final String MALFORMED_EXPRESSION = "#[payload";

  @Test
  @Description("Parse mule expression")
  public void extractMuleExpression() {
    Optional<String> expressionValue = extractExpression(PAYLOAD_EXPRESSION);
    assertThat(expressionValue, is(of("payload")));
  }

  @Test
  @Description("Try parse malformed mule expression")
  public void parseMalformedMuleExpression() {
    Optional<String> expressionValue = extractExpression(MALFORMED_EXPRESSION);
    assertThat(expressionValue, is(empty()));
  }
}
