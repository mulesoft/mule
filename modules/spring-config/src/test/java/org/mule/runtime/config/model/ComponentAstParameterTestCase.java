/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.ast.internal.builder;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ARTIFACT_AST;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ParameterAst.PARAMETER_AST;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.internal.model.DefaultComponentParameterAst;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.declaration.type.annotation.LiteralTypeAnnotation;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Arrays;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(ARTIFACT_AST)
@Story(PARAMETER_AST)
public class ComponentAstParameterTestCase extends AbstractMuleTestCase {

  private final ClassTypeLoader TYPE_LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Test
  @Issue("MULE-19871")
  public void primitiveToNumberWithDefaultValue() {
    assertPrimitiveWithDefaultValue(Long.toString(Long.MAX_VALUE), Long.MAX_VALUE, Number.class);
  }

  @Test
  @Issue("MULE-19871")
  public void primitiveToSmallNumberWithDefaultValue() {
    assertPrimitiveWithDefaultValue(Integer.toString(Integer.MAX_VALUE), Integer.MAX_VALUE, Number.class);
  }

  @Test
  @Issue("MULE-19871")
  public void primitiveToDecimalNumberWithDefaultValue() {
    assertPrimitiveWithDefaultValue("2.53", 2.53, Number.class);
  }

  public enum TestEnum {
    VALUE_A, VALUE_B;
  }

  private void assertPrimitiveWithDefaultValue(String value, Number expected, Class<? extends Number> type) {
    ParameterModel model = mock(ParameterModel.class);
    when(model.getExpressionSupport()).thenReturn(NOT_SUPPORTED);
    when(model.getDefaultValue()).thenReturn(value);
    when(model.getType()).thenReturn(TYPE_LOADER.load(type));

    final DefaultComponentParameterAst parameterAst = new DefaultComponentParameterAst(value, () -> model, null);

    assertThat(parameterAst.getValue().isRight(), is(true));
    assertThat(parameterAst.getValue().getRight(), is(equalTo(expected)));
  }
}
