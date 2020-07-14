/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.model;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.extractExpression;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ARTIFACT_AST;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ParameterAst.PARAMETER_AST;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.internal.model.DefaultComponentParameterAst;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

@Feature(ARTIFACT_AST)
@Story(PARAMETER_AST)
public class ComponentAstParameterTestCase extends AbstractMuleTestCase {

  private final ClassTypeLoader TYPE_LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  private static final String RAW_VALUE = "rawValue";
  private static final String RAW_VALUE_EXPRESSION = "#[payload]";

  @Description("A component parameter with a different raw-value than default model parameter isn't a default parameter")
  @Test
  public void componentParameterWithRawValueIsNonADefaultParameter() {
    ParameterModel model = mock(ParameterModel.class);
    ComponentParameterAst parameterAst = new DefaultComponentParameterAst(RAW_VALUE, () -> model);
    assertThat(parameterAst.isDefaultValue(), is(false));
  }

  @Description("A component parameter without a raw-value is a default parameter")
  @Test
  public void componentParameterWithoutRawValueIsADefaultParameter() {
    ParameterModel model = mock(ParameterModel.class);
    ComponentParameterAst parameterAst = new DefaultComponentParameterAst(null, () -> model);
    assertThat(parameterAst.isDefaultValue(), is(true));
  }

  @Description("A component parameter wit same raw-value as a default model parameter is a default parameter")
  @Test
  public void componentParameterWithSameRawValueAsModelDefaultIsADefaultParameter() {
    ParameterModel model = mock(ParameterModel.class);
    when(model.getDefaultValue()).thenReturn(RAW_VALUE);
    when(model.getType()).thenReturn(TYPE_LOADER.load(String.class));
    ComponentParameterAst parameterAst = new DefaultComponentParameterAst(RAW_VALUE, () -> model);
    assertThat(parameterAst.isDefaultValue(), is(true));
  }

  @Description("A component parameter wit same raw-value expression as a default model parameter is a default parameter")
  @Test
  public void componentParameterWithSameRawValueExpressionAsModelDefaultIsADefaultParameter() {
    ParameterModel model = mock(ParameterModel.class);
    when(model.getDefaultValue()).thenReturn(RAW_VALUE_EXPRESSION);
    when(model.getType()).thenReturn(TYPE_LOADER.load(String.class));
    ComponentParameterAst parameterAst = new DefaultComponentParameterAst(RAW_VALUE_EXPRESSION, () -> model);
    assertThat(parameterAst.isDefaultValue(), is(true));
    assertThat(parameterAst.getValue().getLeft(), is(extractExpression(RAW_VALUE_EXPRESSION).get()));
  }

  @Description("A component parameter with complex value is a default parameter")
  @Test
  public void componentParameterWithComplexValueIsADefaultParameter() {
    ParameterModel model = mock(ParameterModel.class);
    ParameterModel child = mock(ParameterModel.class);
    ComponentParameterAst parameterAst = new DefaultComponentParameterAst(child, () -> model, null);
    assertThat(parameterAst.isDefaultValue(), is(true));
  }
}
