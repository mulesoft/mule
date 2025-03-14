/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.STRICT_STUBS;

import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.impl.DefaultNumberType;
import org.mule.metadata.api.model.impl.DefaultStringType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.module.extension.api.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingException;
import org.mule.runtime.module.extension.internal.loader.java.property.InjectableParameterInfo;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.qameta.allure.Issue;

public class InjectableParameterResolverTestCase extends AbstractMuleTestCase {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule().strictness(STRICT_STUBS);

  @Mock
  private MuleExpressionLanguage expressionManager;
  @Mock
  private ParameterModel parameterModel;
  @Mock
  private ParameterizedModel parameterizedModel;

  @Before
  public void setUp() {
    when(parameterizedModel.getAllParameterModels()).thenReturn(singletonList(parameterModel));
  }

  @Test
  @Issue("W-18030082")
  public void stringValueTrimmed() throws ValueResolvingException {
    final String parameterName = "param";
    when(parameterModel.getName()).thenReturn(parameterName);

    final InjectableParameterInfo injectableParameterInfo = new InjectableParameterInfo(parameterName,
                                                                                        new DefaultStringType(MetadataFormat.JAVA,
                                                                                                              emptyMap()),
                                                                                        true,
                                                                                        parameterName);

    final TypedValue<String> value = new TypedValue<>("   someValue \t\n  ", DataType.STRING);
    final ParameterValueResolver paramValueResolver = mock();
    doReturn(value)
        .when(paramValueResolver).getParameterValue(parameterName);
    doReturn(value)
        .when(expressionManager).evaluate(eq("#[" + parameterName + "_]"), any(DataType.class), any(BindingContext.class));
    final InjectableParameterResolver resolver =
        new InjectableParameterResolver(parameterizedModel, paramValueResolver, expressionManager,
                                        singletonList(injectableParameterInfo));

    assertThat(resolver.getInjectableParameterValue(parameterName), is("   someValue \t\n  ".trim()));
  }

  @Test
  public void numberValue() throws ValueResolvingException {
    final String parameterName = "param";
    when(parameterModel.getName()).thenReturn(parameterName);

    final InjectableParameterInfo injectableParameterInfo = new InjectableParameterInfo(parameterName,
                                                                                        new DefaultNumberType(MetadataFormat.JAVA,
                                                                                                              singletonMap(ClassInformationAnnotation.class,
                                                                                                                           new ClassInformationAnnotation(Integer.class))),
                                                                                        true,
                                                                                        parameterName);

    final TypedValue<Integer> value = new TypedValue<>(99, DataType.NUMBER);
    final ParameterValueResolver paramValueResolver = mock();
    doReturn(value)
        .when(paramValueResolver).getParameterValue(parameterName);
    doReturn(value)
        .when(expressionManager).evaluate(eq("#[" + parameterName + "_]"), any(DataType.class), any(BindingContext.class));

    final InjectableParameterResolver resolver =
        new InjectableParameterResolver(parameterizedModel, paramValueResolver, expressionManager,
                                        singletonList(injectableParameterInfo));

    assertThat(resolver.getInjectableParameterValue(parameterName), is(99));
  }

}
