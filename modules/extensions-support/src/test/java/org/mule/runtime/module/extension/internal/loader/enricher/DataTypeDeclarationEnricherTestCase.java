/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockParameters;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withReturnType;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.extension.api.annotation.DataTypeParameters;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.reflections.ReflectionUtils;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DataTypeDeclarationEnricherTestCase extends AbstractMuleTestCase {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionLoadingContext extensionLoadingContext;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionDeclarer extensionDeclarer;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionDeclaration extensionDeclaration;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private OperationDeclaration annotatedOperation;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private OperationDeclaration notAnnotatedOperation;

  private Method method = getAnnotatedMethod();

  private DataTypeDeclarationEnricher enricher = new DataTypeDeclarationEnricher();

  @Before
  @DataTypeParameters
  public void before() {
    when(extensionLoadingContext.getExtensionDeclarer()).thenReturn(extensionDeclarer);
    when(extensionDeclarer.getDeclaration()).thenReturn(extensionDeclaration);
    when(extensionDeclaration.getOperations()).thenReturn(asList(annotatedOperation, notAnnotatedOperation));
    when(annotatedOperation.getModelProperty(ImplementingTypeModelProperty.class)).thenReturn(empty());
    when(annotatedOperation.getModelProperty(ImplementingMethodModelProperty.class))
        .thenReturn(Optional.of(new ImplementingMethodModelProperty(method)));
    mockParameters(annotatedOperation, new ParameterDeclaration[] {});

    when(notAnnotatedOperation.getModelProperty(ImplementingTypeModelProperty.class)).thenReturn(empty());
    when(notAnnotatedOperation.getModelProperty(ImplementingMethodModelProperty.class)).thenReturn(empty());
    mockParameters(notAnnotatedOperation, new ParameterDeclaration[] {});
  }

  @Test
  public void enrichAnnotated() {
    enricher.enrich(extensionLoadingContext);
    ArgumentCaptor<ParameterDeclaration> captor = ArgumentCaptor.forClass(ParameterDeclaration.class);
    verify(annotatedOperation.getParameterGroups().get(0), times(2)).addParameter(captor.capture());

    assertThat(captor.getAllValues(), hasSize(2));
    assertParameter(captor.getAllValues().get(0), MIME_TYPE_PARAMETER_NAME);
    assertParameter(captor.getAllValues().get(1), ENCODING_PARAMETER_NAME);
  }

  @Test
  public void skipNotAnnotated() {
    enricher.enrich(extensionLoadingContext);
    verify(notAnnotatedOperation.getParameterGroups().get(0), never()).addParameter(any(ParameterDeclaration.class));
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void voidOperation() {
    when(annotatedOperation.getModelProperty(ImplementingMethodModelProperty.class))
        .thenReturn(Optional.of(new ImplementingMethodModelProperty(getVoidAnnotatedMethod())));
    enricher.enrich(extensionLoadingContext);
  }

  private void assertParameter(ParameterDeclaration parameter, String name) {
    assertThat(parameter, is(notNullValue()));
    assertThat(parameter.getName(), is(name));
    assertThat(parameter.getType(), equalTo(toMetadataType(String.class)));
    assertThat(parameter.isRequired(), is(false));
    assertThat(parameter.getExpressionSupport(), is(SUPPORTED));
    assertThat(parameter.getDefaultValue(), is(nullValue()));
  }

  @DataTypeParameters
  public Object operationMethod() {
    return null;
  }

  private Method getAnnotatedMethod() {
    return ReflectionUtils.getMethods(getClass(), withAnnotation(DataTypeParameters.class), withReturnType(Object.class)).stream()
        .findFirst().get();
  }

  private Method getVoidAnnotatedMethod() {
    return ReflectionUtils.getMethods(getClass(), withAnnotation(DataTypeParameters.class), withReturnType(void.class)).stream()
        .findFirst().get();
  }
}
