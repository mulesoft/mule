/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.OUTPUT;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getNamedObject;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OutputDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.extension.api.ExtensionConstants;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.enricher.TargetParameterDeclarationEnricher;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TargetParameterDeclarationEnricherTestCase {

  public static final String PAYLOAD_EXPRESSION = "#[payload]";
  public static final String TEST_VALUE = "testValue";
  public static final String TEST_EXPRESSION = "#[testExpression]";
  private ExtensionDeclarer extensionDeclarer;
  private final ParameterGroupDeclaration parameterGroupDeclaration;
  private final boolean isEmptyOutputParameterGroup;

  @Parameterized.Parameters
  public static Collection<ParameterGroupDeclaration> getParameters() {
    return asList(getOutputParameterGroupDeclarationStub(), new ParameterGroupDeclaration(OUTPUT));
  }

  public TargetParameterDeclarationEnricherTestCase(ParameterGroupDeclaration parameterGroupDeclaration) {
    this.parameterGroupDeclaration = parameterGroupDeclaration;
    this.isEmptyOutputParameterGroup = parameterGroupDeclaration.getParameters().isEmpty();
  }

  @Before
  public void setUp() {
    OperationDeclaration operationDeclaration = mock(OperationDeclaration.class);
    OutputDeclaration outputDeclaration = mock(OutputDeclaration.class);
    when(outputDeclaration.getType()).thenReturn(mock(StringType.class));
    when(operationDeclaration.getOutput()).thenReturn(outputDeclaration);
    when(operationDeclaration.getName()).thenReturn("mockedOperation");
    when(operationDeclaration.getParameterGroup(OUTPUT)).thenReturn(parameterGroupDeclaration);
    OperationDeclarer operationDeclarer = mock(OperationDeclarer.class);
    when(operationDeclarer.getDeclaration()).thenReturn(operationDeclaration);
    extensionDeclarer = new ExtensionDeclarer().named("mockedExtension");
    extensionDeclarer.withOperation(operationDeclarer);
  }

  private static ParameterGroupDeclaration getOutputParameterGroupDeclarationStub() {
    ParameterGroupDeclaration parameterGroupDeclarationStub = new ParameterGroupDeclaration(OUTPUT);
    ParameterDeclaration targetParameterDeclarationStub = new ParameterDeclaration(ExtensionConstants.TARGET_PARAMETER_NAME);
    targetParameterDeclarationStub.setDefaultValue(TEST_VALUE);
    targetParameterDeclarationStub.setExpressionSupport(NOT_SUPPORTED);
    targetParameterDeclarationStub.setType(mock(StringType.class), false);
    targetParameterDeclarationStub.setRequired(false);
    parameterGroupDeclarationStub.addParameter(targetParameterDeclarationStub);
    // targetValue parameter setup
    ParameterDeclaration targetValueParameterDeclarationStub =
        new ParameterDeclaration(ExtensionConstants.TARGET_VALUE_PARAMETER_NAME);
    targetValueParameterDeclarationStub.setRequired(false);
    targetValueParameterDeclarationStub.setExpressionSupport(REQUIRED);
    targetValueParameterDeclarationStub.setType(mock(StringType.class), false);
    targetValueParameterDeclarationStub.setDefaultValue(TEST_EXPRESSION);
    parameterGroupDeclarationStub.addParameter(targetValueParameterDeclarationStub);
    return parameterGroupDeclarationStub;
  }

  @Test
  public void verifyOperationEnrichment() {
    new TargetParameterDeclarationEnricher()
        .enrich(new DefaultExtensionLoadingContext(extensionDeclarer, getClass().getClassLoader(), getDefault(emptySet())));
    assertThat(parameterGroupDeclaration.getName(), is(OUTPUT));
    assertTargetParameterEnrichment();
    assertTargetValueEnrichment();
  }

  private void assertTargetParameterEnrichment() {
    ParameterDeclaration targetParameterDeclaration =
        getNamedObject(parameterGroupDeclaration.getParameters(), ExtensionConstants.TARGET_PARAMETER_NAME);
    assertThat(targetParameterDeclaration.getType(), instanceOf(StringType.class));
    assertThat(targetParameterDeclaration.isRequired(), is(false));
    assertThat(targetParameterDeclaration.getExpressionSupport(), is(NOT_SUPPORTED));
    if (isEmptyOutputParameterGroup) {
      // The enricher should add the parameter with the default value.
      assertThat(targetParameterDeclaration.getDefaultValue(), is(nullValue()));
    } else {
      // The enricher should leave the previously set parameter as it is.
      assertThat(targetParameterDeclaration.getDefaultValue(), is(TEST_VALUE));
    }
  }

  private void assertTargetValueEnrichment() {
    ParameterDeclaration targetParameterDeclaration =
        getNamedObject(parameterGroupDeclaration.getParameters(), ExtensionConstants.TARGET_VALUE_PARAMETER_NAME);
    assertThat(targetParameterDeclaration.getType(), instanceOf(StringType.class));
    assertThat(targetParameterDeclaration.isRequired(), is(false));
    assertThat(targetParameterDeclaration.getExpressionSupport(), is(REQUIRED));
    if (isEmptyOutputParameterGroup) {
      // The enricher should add the parameter with the default value.
      assertThat(targetParameterDeclaration.getDefaultValue(), is(PAYLOAD_EXPRESSION));
    } else {
      // The enricher should leave the previously set parameter as it is.
      assertThat(targetParameterDeclaration.getDefaultValue(), is(TEST_EXPRESSION));
    }
  }
}
