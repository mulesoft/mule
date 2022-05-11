/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.TypeConverter;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.config.BeanDefinition;

import org.junit.Before;
import org.junit.Test;

import org.mockito.ArgumentCaptor;

public class SimpleTypeBeanDefinitionCreatorTestCase extends AbstractMuleTestCase {

  public static final String TEST_STRING_RAW_VALUE = "Test Raw Value";
  private SimpleTypeBeanComponentDefinitionCreator simpleTypeBeanDefinitionCreator;
  private Map<ComponentAst, SpringComponentModel> springComponentModels;
  private CreateComponentBeanDefinitionRequest createBeanDefinitionRequest;

  @Before
  public void setUp() {
    simpleTypeBeanDefinitionCreator = new SimpleTypeBeanComponentDefinitionCreator();
    createBeanDefinitionRequest = mock(CreateComponentBeanDefinitionRequest.class);
    SpringComponentModel springComponentModel = new SpringComponentModel();
    when(createBeanDefinitionRequest.getSpringComponentModel()).thenReturn(springComponentModel);
  }

  @Test
  public void testHandleRequestReturnsFalse_WhenCreateBeanDefinitionRequestTypeIsNotSimpleType() {
    // Given
    SpringComponentModel springComponentModel = createBeanDefinitionRequest.getSpringComponentModel();
    springComponentModel.setType(SimpleTypeBeanDefinitionCreatorTestCase.class);

    // When
    boolean result = simpleTypeBeanDefinitionCreator.handleRequest(springComponentModels, createBeanDefinitionRequest);

    // Then
    assertThat(result, is(false));
  }

  @Test
  public void testHandleRequestReturnsTrue_WhenCreateBeanDefinitionRequestTypeIsSimpleTypeAndParamFoundInOwnerComponent() {
    // Given
    SpringComponentModel springComponentModel = createBeanDefinitionRequest.getSpringComponentModel();
    springComponentModel.setType(String.class);

    ComponentParameterAst paramInOwnerComponent = mock(ComponentParameterAst.class);
    when(paramInOwnerComponent.getResolvedRawValue()).thenReturn(TEST_STRING_RAW_VALUE);

    ComponentAst componentModel = mock(ComponentAst.class);
    when(componentModel.getParameter(DEFAULT_GROUP_NAME, "value")).thenReturn(paramInOwnerComponent);
    when(createBeanDefinitionRequest.getComponent()).thenReturn(componentModel);

    mockComponentBuildingDefinition(createBeanDefinitionRequest, empty());

    // When
    boolean result = simpleTypeBeanDefinitionCreator.handleRequest(springComponentModels, createBeanDefinitionRequest);

    // Then
    assertThat(result, is(true));
  }

  @Test
  public void testHandleRequestCallsSetBeanDefinitionWithResultGetConvertibleBeanDefinition_WhenCreateBeanDefinitionRequestTypeIsSimpleTypeAndParamFoundInOwnerComponentAndNoTypeConverter() {
    // Given
    SpringComponentModel springComponentModel = mock(SpringComponentModel.class);
    when(springComponentModel.getType()).then(inv -> String.class);
    when(createBeanDefinitionRequest.getSpringComponentModel()).thenReturn(springComponentModel);

    ComponentParameterAst paramInOwnerComponent = mock(ComponentParameterAst.class);
    when(paramInOwnerComponent.getResolvedRawValue()).thenReturn(TEST_STRING_RAW_VALUE);

    ComponentAst componentModel = mock(ComponentAst.class);
    when(componentModel.getParameter(DEFAULT_GROUP_NAME, "value")).thenReturn(paramInOwnerComponent);
    when(createBeanDefinitionRequest.getComponent()).thenReturn(componentModel);

    mockComponentBuildingDefinition(createBeanDefinitionRequest, empty());

    // When
    simpleTypeBeanDefinitionCreator.handleRequest(springComponentModels, createBeanDefinitionRequest);

    // Then
    ArgumentCaptor<BeanDefinition> beanDefinitionArgumentCaptor = forClass(BeanDefinition.class);
    verify(springComponentModel, times(1)).setBeanDefinition(beanDefinitionArgumentCaptor.capture());
    BeanDefinition value = beanDefinitionArgumentCaptor.getValue();
    assertThat(value.getBeanClassName(), is(String.class.getName()));
    assertThat(value.getConstructorArgumentValues().getArgumentValue(0, String.class).getValue(), is(TEST_STRING_RAW_VALUE));
  }

  @Test
  public void testHandleRequestCallsSetBeanDefinitionWithResultGetConvertibleBeanDefinition_WhenCreateBeanDefinitionRequestTypeIsSimpleTypeAndParamFoundInOwnerComponentAndTypeConverter() {
    // Given
    SpringComponentModel springComponentModel = mock(SpringComponentModel.class);
    when(springComponentModel.getType()).then(inv -> String.class);
    when(createBeanDefinitionRequest.getSpringComponentModel()).thenReturn(springComponentModel);

    ComponentParameterAst paramInOwnerComponent = mock(ComponentParameterAst.class);
    when(paramInOwnerComponent.getResolvedRawValue()).thenReturn(TEST_STRING_RAW_VALUE);

    ComponentAst componentModel = mock(ComponentAst.class);
    when(componentModel.getParameter(DEFAULT_GROUP_NAME, "value")).thenReturn(paramInOwnerComponent);
    when(createBeanDefinitionRequest.getComponent()).thenReturn(componentModel);

    mockComponentBuildingDefinition(createBeanDefinitionRequest, of(o -> TEST_STRING_RAW_VALUE));

    // When
    simpleTypeBeanDefinitionCreator.handleRequest(springComponentModels, createBeanDefinitionRequest);

    // Then
    ArgumentCaptor<BeanDefinition> beanDefinitionArgumentCaptor = forClass(BeanDefinition.class);
    verify(springComponentModel, times(1)).setBeanDefinition(beanDefinitionArgumentCaptor.capture());
    BeanDefinition value = beanDefinitionArgumentCaptor.getValue();
    assertThat(value.getBeanClassName(), is("org.mule.runtime.config.internal.factories.ConstantFactoryBean"));
    assertThat(value.getConstructorArgumentValues().getArgumentValue(0, String.class).getValue(), is(TEST_STRING_RAW_VALUE));
  }

  @Test
  public void testHandleRequestReturnsTrue_WhenCreateBeanDefinitionRequestTypeIsSimpleTypeAndParamNotFoundInOwnerComponentButFoundInComponentModel() {
    // Given
    SpringComponentModel springComponentModel = createBeanDefinitionRequest.getSpringComponentModel();
    springComponentModel.setType(String.class);

    ComponentAst componentModel = mock(ComponentAst.class);
    ComponentParameterAst componentParameterAst = mock(ComponentParameterAst.class);
    when(componentParameterAst.getResolvedRawValue()).thenReturn(TEST_STRING_RAW_VALUE);

    when(componentModel.getParameter(DEFAULT_GROUP_NAME, "value")).thenReturn(componentParameterAst);
    when(createBeanDefinitionRequest.getComponent()).thenReturn(componentModel);

    mockComponentBuildingDefinition(createBeanDefinitionRequest, empty());

    // When
    boolean result = simpleTypeBeanDefinitionCreator.handleRequest(springComponentModels, createBeanDefinitionRequest);

    // Then
    assertThat(result, is(true));
  }

  @Test
  public void testHandleRequestCallsSetBeanDefinitionWithResultGetConvertibleBeanDefinition_WhenCreateBeanDefinitionRequestTypeIsSimpleTypeAndParamNotFoundInOwnerComponentButFoundInComponentModelAndNoTypeConverter() {
    // Given
    SpringComponentModel springComponentModel = mock(SpringComponentModel.class);
    when(springComponentModel.getType()).then(inv -> String.class);
    when(createBeanDefinitionRequest.getSpringComponentModel()).thenReturn(springComponentModel);

    ComponentAst componentModel = mock(ComponentAst.class);
    ComponentParameterAst componentParameterAst = mock(ComponentParameterAst.class);
    when(componentParameterAst.getResolvedRawValue()).thenReturn(TEST_STRING_RAW_VALUE);

    when(componentModel.getParameter(DEFAULT_GROUP_NAME, "value")).thenReturn(componentParameterAst);
    when(createBeanDefinitionRequest.getComponent()).thenReturn(componentModel);

    mockComponentBuildingDefinition(createBeanDefinitionRequest, empty());

    // When
    simpleTypeBeanDefinitionCreator.handleRequest(springComponentModels, createBeanDefinitionRequest);

    // Then
    ArgumentCaptor<BeanDefinition> beanDefinitionArgumentCaptor = forClass(BeanDefinition.class);
    verify(springComponentModel, times(1)).setBeanDefinition(beanDefinitionArgumentCaptor.capture());
    BeanDefinition value = beanDefinitionArgumentCaptor.getValue();
    assertThat(value.getBeanClassName(), is(String.class.getName()));
    assertThat(value.getConstructorArgumentValues().getArgumentValue(0, String.class).getValue(), is(TEST_STRING_RAW_VALUE));
  }

  @Test
  public void testHandleRequestCallsSetBeanDefinitionWithResultGetConvertibleBeanDefinition_WhenCreateBeanDefinitionRequestTypeIsSimpleTypeAndParamNotFoundInOwnerComponentButFoundInComponentModelAndTypeConverter() {
    // Given
    SpringComponentModel springComponentModel = mock(SpringComponentModel.class);
    when(springComponentModel.getType()).then(inv -> String.class);
    when(createBeanDefinitionRequest.getSpringComponentModel()).thenReturn(springComponentModel);

    ComponentAst componentModel = mock(ComponentAst.class);
    ComponentParameterAst componentParameterAst = mock(ComponentParameterAst.class);
    when(componentParameterAst.getResolvedRawValue()).thenReturn(TEST_STRING_RAW_VALUE);

    when(componentModel.getParameter(DEFAULT_GROUP_NAME, "value")).thenReturn(componentParameterAst);
    when(createBeanDefinitionRequest.getComponent()).thenReturn(componentModel);

    mockComponentBuildingDefinition(createBeanDefinitionRequest, of(o -> TEST_STRING_RAW_VALUE));

    // When
    simpleTypeBeanDefinitionCreator.handleRequest(springComponentModels, createBeanDefinitionRequest);

    // Then
    ArgumentCaptor<BeanDefinition> beanDefinitionArgumentCaptor = forClass(BeanDefinition.class);
    verify(springComponentModel, times(1)).setBeanDefinition(beanDefinitionArgumentCaptor.capture());
    BeanDefinition value = beanDefinitionArgumentCaptor.getValue();
    assertThat(value.getBeanClassName(), is("org.mule.runtime.config.internal.factories.ConstantFactoryBean"));
    assertThat(value.getConstructorArgumentValues().getArgumentValue(0, String.class).getValue(), is(TEST_STRING_RAW_VALUE));
  }

  private void mockComponentBuildingDefinition(CreateBeanDefinitionRequest createBeanDefinitionRequest,
                                               Optional<TypeConverter> typeConverterOptional) {
    ComponentBuildingDefinition componentBuildingDefinition = mock(ComponentBuildingDefinition.class);
    when(componentBuildingDefinition.getTypeConverter()).thenReturn(typeConverterOptional);
    when(createBeanDefinitionRequest.getComponentBuildingDefinition()).thenReturn(componentBuildingDefinition);
  }

}
