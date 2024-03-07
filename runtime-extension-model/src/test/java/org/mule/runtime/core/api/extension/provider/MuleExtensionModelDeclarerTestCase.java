/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.extension.provider;

import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static java.util.stream.Collectors.toList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
@RunWith(Parameterized.class)
public class MuleExtensionModelDeclarerTestCase {

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

    assertThat(expressionSupport.name(), is("NOT_SUPPORTED"));
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
    assertThat(parseTemplateOperation.getParameterGroup(ParameterGroupModel.OUTPUT).getParameters().stream()
        .filter(parameterDeclaration -> parameterDeclaration.getName().equals("targetValue")).collect(toList()), is(empty()));
  }
}
