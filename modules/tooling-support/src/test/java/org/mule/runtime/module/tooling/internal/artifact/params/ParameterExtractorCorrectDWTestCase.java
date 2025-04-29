/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact.params;

import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue.plain;
import static org.mule.runtime.module.tooling.internal.artifact.params.ParameterExtractor.asDataWeaveExpression;
import static org.mule.tck.junit4.rule.DataWeaveExpressionLanguage.dataWeaveRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DataWeaveExpressionLanguage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ParameterExtractorCorrectDWTestCase extends AbstractMuleTestCase {

  @Rule
  public DataWeaveExpressionLanguage dw = dataWeaveRule();

  private ExpressionManager expressionManager;

  @Before
  public void setUp() throws Exception {
    expressionManager = dw.getExpressionManager();
  }

  @Test
  public void simpleString() {
    generateDeclarationAndEvaluate("This is a simple string");
  }

  @Test
  public void doubleQuotes() {
    generateDeclarationAndEvaluate("This is \"not\" so simple");
  }

  @Test
  public void singleQuotes() {
    generateDeclarationAndEvaluate("This is 'not' so simple");
  }

  @Test
  public void escapedSingleQuotes() {
    generateDeclarationAndEvaluate("This is \\'not\\' so simple");
  }

  @Test
  public void multipleEscapesInSingleQuotes() {
    generateDeclarationAndEvaluate("This is \\\\\'not\\\\\' so simple");
  }

  @Test
  public void escapedDoubleQuotes() {
    generateDeclarationAndEvaluate("This is \\\"not\\\" so simple");
  }

  @Test
  public void multipleEscapesInDoubleQuotes() {
    generateDeclarationAndEvaluate("This is \\\\\"not\\\\\" so simple");
  }

  @Test
  public void multipleDoubleQuotes() {
    generateDeclarationAndEvaluate("\"This\" \"is\" \"not\" \"so\" \"simple\"");
  }

  @Test
  public void multipleSingleQuotes() {
    generateDeclarationAndEvaluate("'This' 'is' 'not' 'so' 'simple'");
  }

  @Test
  public void singleQuoteXML() {
    generateDeclarationAndEvaluate("<?xml version='1.0' encoding='UTF-8' standalone='yes'?> <tag>tagValue</tag>");
  }

  @Test
  public void combinedQuotes() {
    generateDeclarationAndEvaluate("'single' and \"double\" quotes");
  }

  @Test
  public void doubleQuoteXML() {
    generateDeclarationAndEvaluate("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?> <tag>tagValue</tag>");
  }

  @Test
  public void doubleQuotesJson() {
    generateDeclarationAndEvaluate("{\"property\": \"value\"}");
  }

  private void generateDeclarationAndEvaluate(String parameterInput) {
    TypedValue<?> dwExpression = asDataWeaveExpression(plain(parameterInput));
    final String bindingVariable = "bindingVariable";
    BindingContext context = BindingContext.builder().addBinding(bindingVariable, dwExpression).build();
    TypedValue<?> outputValue = expressionManager.evaluate("#[" + bindingVariable + "]", STRING, context);
    assertThat(outputValue.getValue(), equalTo(parameterInput));
  }

}
