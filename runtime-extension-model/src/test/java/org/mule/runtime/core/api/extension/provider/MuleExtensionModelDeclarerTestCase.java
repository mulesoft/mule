/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.extension.provider;

import static org.mule.runtime.api.util.MuleSystemProperties.PARSE_TEMPLATE_USE_LEGACY_DEFAULT_TARGET_VALUE;
import static org.mule.runtime.api.util.MuleSystemProperties.REVERT_SUPPORT_EXPRESSIONS_IN_VARIABLE_NAME_IN_SET_VARIABLE_PROPERTY;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;

import java.util.Collection;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
@RunWith(Parameterized.class)
public class MuleExtensionModelDeclarerTestCase {

  private final String expectedResult;

  private String isDoNotSupportExpressionsOriginalValue;
  private final String isDoNotSupportExpressionsValue;
  private String isParseTemplateUseLegacyDefaultTargetValueOriginalValue;
  private final String isParseTemplateUseLegacyDefaultTargetValue;

  public MuleExtensionModelDeclarerTestCase(String isDoNotSupportExpressions, String expectedResult,
                                            String isParseTemplateUseLegacyDefaultTargetValue) {
    this.isDoNotSupportExpressionsValue = isDoNotSupportExpressions;
    this.expectedResult = expectedResult;
    this.isParseTemplateUseLegacyDefaultTargetValue = isParseTemplateUseLegacyDefaultTargetValue;
  }

  // TODO W-13974116: Remove circular dependency when adding org.mule.tests:mule-tests-unit to this module
  @Before
  public void setUp() {
    isDoNotSupportExpressionsOriginalValue =
        setProperty(REVERT_SUPPORT_EXPRESSIONS_IN_VARIABLE_NAME_IN_SET_VARIABLE_PROPERTY, isDoNotSupportExpressionsValue);
    isParseTemplateUseLegacyDefaultTargetValueOriginalValue =
        setProperty(PARSE_TEMPLATE_USE_LEGACY_DEFAULT_TARGET_VALUE, isParseTemplateUseLegacyDefaultTargetValue);
  }

  @After
  public void tearDown() {
    if (isDoNotSupportExpressionsOriginalValue == null) {
      clearProperty(REVERT_SUPPORT_EXPRESSIONS_IN_VARIABLE_NAME_IN_SET_VARIABLE_PROPERTY);
    } else {
      setProperty(REVERT_SUPPORT_EXPRESSIONS_IN_VARIABLE_NAME_IN_SET_VARIABLE_PROPERTY, isDoNotSupportExpressionsOriginalValue);

    }
    if (isParseTemplateUseLegacyDefaultTargetValueOriginalValue == null) {
      clearProperty(PARSE_TEMPLATE_USE_LEGACY_DEFAULT_TARGET_VALUE);
    } else {
      setProperty(PARSE_TEMPLATE_USE_LEGACY_DEFAULT_TARGET_VALUE, isDoNotSupportExpressionsOriginalValue);
    }
  }

  @Parameterized.Parameters(
      name = "Do not support expressions in variableName in SetVariable: {0} - Use parseTemplate legacy default target value: {2}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"true", "NOT_SUPPORTED", "true"},
        {"false", "SUPPORTED", "false"}
    });
  }

  @Test
  @Issue("W-10998630")
  public void whenCreatingExtensionModelVariableNameShouldSupportExpressionsAccordingToSystemProperty() {
    MuleExtensionModelDeclarer muleExtensionModelDeclarer = new MuleExtensionModelDeclarer();
    ExtensionDeclarer extensionDeclarer = muleExtensionModelDeclarer.createExtensionModel();
    OperationDeclaration setVariableOperation = extensionDeclarer.getDeclaration().getOperations().stream()
        .filter(operationDeclaration -> operationDeclaration.getName().equals("setVariable")).findFirst().get();
    ParameterDeclaration setVariableParameter = setVariableOperation.getParameterGroup("General").getParameters().stream()
        .filter(parameterDeclaration -> parameterDeclaration.getName().equals("variableName")).findFirst().get();
    ExpressionSupport expressionSupport = setVariableParameter.getExpressionSupport();

    assertThat(expressionSupport.name(), is(expectedResult));
  }

  @Test
  @Issue("W-13965819")
  public void whenIsParseTemplateUseLegacyDefaultTargetValueTheTargetValueIsMessage() {
    MuleExtensionModelDeclarer muleExtensionModelDeclarer = new MuleExtensionModelDeclarer();
    ExtensionDeclarer extensionDeclarer = muleExtensionModelDeclarer.createExtensionModel();
    OperationDeclaration parseTemplateOperation = extensionDeclarer.getDeclaration().getOperations().stream()
        .filter(operationDeclaration -> operationDeclaration.getName().equals("parseTemplate")).findFirst().get();
    assertTargetValueParameter(parseTemplateOperation);
  }

  private void assertTargetValueParameter(OperationDeclaration parseTemplateOperation) {
    if (parseBoolean(isParseTemplateUseLegacyDefaultTargetValue)) {
      assertThat(parseTemplateOperation.getParameterGroup(ParameterGroupModel.OUTPUT).getParameters().stream()
          .filter(parameterDeclaration -> parameterDeclaration.getName().equals("targetValue")).findFirst().get()
          .getDefaultValue(),
                 equalTo("#[message]"));
    } else {
      assertThat(parseTemplateOperation.getParameterGroup(ParameterGroupModel.OUTPUT).getParameters().stream()
          .filter(parameterDeclaration -> parameterDeclaration.getName().equals("targetValue")).collect(toList()), is(empty()));
    }
  }
}
