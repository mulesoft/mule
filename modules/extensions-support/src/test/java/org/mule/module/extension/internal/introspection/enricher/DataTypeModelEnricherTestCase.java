/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.enricher;

import static java.util.Arrays.asList;
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
import static org.mule.extension.api.introspection.ExpressionSupport.SUPPORTED;
import static org.mule.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withReturnType;
import org.mule.extension.annotation.api.DataTypeParameters;
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.introspection.DataType;
import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.reflections.ReflectionUtils;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DataTypeModelEnricherTestCase extends AbstractMuleTestCase
{

    @Mock(answer = RETURNS_DEEP_STUBS)
    private DescribingContext describingContext;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private OperationDeclaration annotatedOperation;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private OperationDeclaration notAnnotatedOperation;

    private Method method = getAnnotatedMethod();

    private DataTypeModelEnricher enricher = new DataTypeModelEnricher();

    @Before
    @DataTypeParameters
    public void before()
    {
        when(describingContext.getDeclarationDescriptor().getDeclaration().getOperations()).thenReturn(asList(annotatedOperation, notAnnotatedOperation));
        when(annotatedOperation.getModelProperty(ImplementingTypeModelProperty.KEY)).thenReturn(null);
        when(notAnnotatedOperation.getModelProperty(ImplementingTypeModelProperty.KEY)).thenReturn(null);
        when(annotatedOperation.getModelProperty(ImplementingMethodModelProperty.KEY)).thenReturn(new ImplementingMethodModelProperty(method));
        when(notAnnotatedOperation.getModelProperty(ImplementingMethodModelProperty.KEY)).thenReturn(null);
    }

    @Test
    public void enrichAnnotated()
    {
        enricher.enrich(describingContext);
        ArgumentCaptor<ParameterDeclaration> captor = ArgumentCaptor.forClass(ParameterDeclaration.class);
        verify(annotatedOperation, times(2)).addParameter(captor.capture());

        assertThat(captor.getAllValues(), hasSize(2));
        assertParameter(captor.getAllValues().get(0), MIME_TYPE_PARAMETER_NAME);
        assertParameter(captor.getAllValues().get(1), ENCODING_PARAMETER_NAME);
    }

    @Test
    public void skipNotAnnotated()
    {
        enricher.enrich(describingContext);
        verify(notAnnotatedOperation, never()).addParameter(any(ParameterDeclaration.class));
    }

    @Test(expected = IllegalModelDefinitionException.class)
    public void voidOperation()
    {
        when(annotatedOperation.getModelProperty(ImplementingMethodModelProperty.KEY)).thenReturn(new ImplementingMethodModelProperty(getVoidAnnotatedMethod()));
        enricher.enrich(describingContext);
    }

    private void assertParameter(ParameterDeclaration parameter, String name)
    {
        assertThat(parameter, is(notNullValue()));
        assertThat(parameter.getName(), is(name));
        assertThat(parameter.getType(), equalTo(DataType.of(String.class)));
        assertThat(parameter.isRequired(), is(false));
        assertThat(parameter.getExpressionSupport(), is(SUPPORTED));
        assertThat(parameter.getDefaultValue(), is(nullValue()));
    }

    @DataTypeParameters
    public Object operationMethod() {
        return null;
    }

    private Method getAnnotatedMethod()
    {
        return ReflectionUtils.getMethods(getClass(), withAnnotation(DataTypeParameters.class),
                                          withReturnType(Object.class))
                .stream().findFirst().get();
    }

    private Method getVoidAnnotatedMethod()
    {
        return ReflectionUtils.getMethods(getClass(), withAnnotation(DataTypeParameters.class),
                                          withReturnType(void.class))
                .stream().findFirst().get();
    }
}
