/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getDefaultValue;
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
  private static final String DEFAULT_VALUE = "DEFAULT_VALUE";

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

  @Test
  public void deprecatedOptionalWithDefaultValue() {
    org.mule.runtime.extension.api.annotation.param.Optional optional =
        mock(org.mule.runtime.extension.api.annotation.param.Optional.class);
    when(optional.defaultValue()).thenReturn(DEFAULT_VALUE);
    assertOptional(getDefaultValue(optional));
  }

  @Test
  public void optionalWithDefaultValue() {
    org.mule.sdk.api.annotation.param.Optional optional = mock(org.mule.sdk.api.annotation.param.Optional.class);
    when(optional.defaultValue()).thenReturn(DEFAULT_VALUE);
    assertOptional(getDefaultValue(optional));
  }

  private void assertOptional(Optional<String> defaultValue) {
    assertThat(defaultValue.isPresent(), is(true));
    assertThat(defaultValue.get(), is(DEFAULT_VALUE));
  }
}
