package org.mule.runtime.config.internal.dsl.spring;

import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentGenerationInformation;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.dsl.processor.ObjectTypeVisitor;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.TypeConverter;
import org.springframework.beans.factory.config.BeanDefinition;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SimpleTypeBeanDefinitionCreatorTestCase {

  public static final String TEST_STRING_RAW_VALUE = "Test Raw Value";
  private SimpleTypeBeanDefinitionCreator simpleTypeBeanDefinitionCreator;
  private Map<ComponentAst, SpringComponentModel> springComponentModels;
  private ParameterUtils parameterUtils;
  private CreateBeanDefinitionRequest createBeanDefinitionRequest;

  @Before
  public void setUp() {
    parameterUtils = mock(ParameterUtils.class);
    simpleTypeBeanDefinitionCreator = new SimpleTypeBeanDefinitionCreator(parameterUtils);
    createBeanDefinitionRequest = mock(CreateBeanDefinitionRequest.class);
    SpringComponentModel springComponentModel = new SpringComponentModel();
    when(createBeanDefinitionRequest.getSpringComponentModel()).thenReturn(springComponentModel);
  }

  @Test
  public void testHandleRequestReturnsFalse_WhenCreateBeanDefinitionRequestTypeIsNotSimpleType() {
    // Given
    mockStringObjectTypeVisitor(createBeanDefinitionRequest, SimpleTypeBeanDefinitionCreatorTestCase.class);

    // When
    boolean result = simpleTypeBeanDefinitionCreator.handleRequest(springComponentModels, createBeanDefinitionRequest);

    // Then
    assertThat(result, is(false));
  }

  @Test
  public void testHandleRequestReturnsTrue_WhenCreateBeanDefinitionRequestTypeIsSimpleTypeAndParamFoundInOwnerComponent() {
    // Given
    mockStringObjectTypeVisitor(createBeanDefinitionRequest, String.class);

    ComponentParameterAst paramInOwnerComponent = new MyComponentParameterAst();

    when(parameterUtils.getParamInOwnerComponent(createBeanDefinitionRequest)).thenReturn(paramInOwnerComponent);

    mockComponentBuildingDefinition(createBeanDefinitionRequest, Optional.empty());

    // When
    boolean result = simpleTypeBeanDefinitionCreator.handleRequest(springComponentModels, createBeanDefinitionRequest);

    // Then
    assertThat(result, is(true));
  }

  @Test
  public void testHandleRequestCallsSetBeanDefinitionWithResultGetConvertibleBeanDefinition_WhenCreateBeanDefinitionRequestTypeIsSimpleTypeAndParamFoundInOwnerComponentAndNoTypeConverter() {
    // Given
    mockStringObjectTypeVisitor(createBeanDefinitionRequest, String.class);

    SpringComponentModel springComponentModel = mock(SpringComponentModel.class);
    when(createBeanDefinitionRequest.getSpringComponentModel()).thenReturn(springComponentModel);

    ComponentParameterAst paramInOwnerComponent = new MyComponentParameterAst();

    when(parameterUtils.getParamInOwnerComponent(createBeanDefinitionRequest)).thenReturn(paramInOwnerComponent);

    mockComponentBuildingDefinition(createBeanDefinitionRequest, Optional.empty());

    // When
    simpleTypeBeanDefinitionCreator.handleRequest(springComponentModels, createBeanDefinitionRequest);

    // Then
    ArgumentCaptor<BeanDefinition> beanDefinitionArgumentCaptor = ArgumentCaptor.forClass(BeanDefinition.class);
    verify(springComponentModel, times(1)).setBeanDefinition(beanDefinitionArgumentCaptor.capture());
    BeanDefinition value = beanDefinitionArgumentCaptor.getValue();
    assertThat(value.getBeanClassName(), is(String.class.getName()));
    assertThat(value.getConstructorArgumentValues().getArgumentValue(0, String.class).getValue(), is(TEST_STRING_RAW_VALUE));
  }

  @Test
  public void testHandleRequestCallsSetBeanDefinitionWithResultGetConvertibleBeanDefinition_WhenCreateBeanDefinitionRequestTypeIsSimpleTypeAndParamFoundInOwnerComponentAndTypeConverter() {
    // Given
    mockStringObjectTypeVisitor(createBeanDefinitionRequest, String.class);

    SpringComponentModel springComponentModel = mock(SpringComponentModel.class);
    when(createBeanDefinitionRequest.getSpringComponentModel()).thenReturn(springComponentModel);

    ComponentParameterAst paramInOwnerComponent = new MyComponentParameterAst();

    when(parameterUtils.getParamInOwnerComponent(createBeanDefinitionRequest)).thenReturn(paramInOwnerComponent);

    mockComponentBuildingDefinition(createBeanDefinitionRequest, Optional.of(o -> TEST_STRING_RAW_VALUE));

    // When
    simpleTypeBeanDefinitionCreator.handleRequest(springComponentModels, createBeanDefinitionRequest);

    // Then
    ArgumentCaptor<BeanDefinition> beanDefinitionArgumentCaptor = ArgumentCaptor.forClass(BeanDefinition.class);
    verify(springComponentModel, times(1)).setBeanDefinition(beanDefinitionArgumentCaptor.capture());
    BeanDefinition value = beanDefinitionArgumentCaptor.getValue();
    assertThat(value.getBeanClassName(), is("org.mule.runtime.config.internal.factories.ConstantFactoryBean"));
    assertThat(value.getConstructorArgumentValues().getArgumentValue(0, String.class).getValue(), is(TEST_STRING_RAW_VALUE));
  }

  @Test
  public void testHandleRequestReturnsTrue_WhenCreateBeanDefinitionRequestTypeIsSimpleTypeAndParamNotFoundInOwnerComponentButFoundInComponentModel() {
    // Given
    mockStringObjectTypeVisitor(createBeanDefinitionRequest, String.class);

    when(parameterUtils.getParamInOwnerComponent(createBeanDefinitionRequest)).thenReturn(null);

    ComponentAst componentModel = mock(ComponentAst.class);
    ComponentParameterAst componentParameterAst = mock(ComponentParameterAst.class);
    when(componentParameterAst.getResolvedRawValue()).thenReturn(TEST_STRING_RAW_VALUE);

    when(componentModel.getParameter("value")).thenReturn(componentParameterAst);
    when(createBeanDefinitionRequest.getComponentModel()).thenReturn(componentModel);

    mockComponentBuildingDefinition(createBeanDefinitionRequest, Optional.empty());

    // When
    boolean result = simpleTypeBeanDefinitionCreator.handleRequest(springComponentModels, createBeanDefinitionRequest);

    // Then
    assertThat(result, is(true));
  }

  @Test
  public void testHandleRequestCallsSetBeanDefinitionWithResultGetConvertibleBeanDefinition_WhenCreateBeanDefinitionRequestTypeIsSimpleTypeAndParamNotFoundInOwnerComponentButFoundInComponentModelAndNoTypeConverter() {
    // Given
    mockStringObjectTypeVisitor(createBeanDefinitionRequest, String.class);

    SpringComponentModel springComponentModel = mock(SpringComponentModel.class);
    when(createBeanDefinitionRequest.getSpringComponentModel()).thenReturn(springComponentModel);

    when(parameterUtils.getParamInOwnerComponent(createBeanDefinitionRequest)).thenReturn(null);

    ComponentAst componentModel = mock(ComponentAst.class);
    ComponentParameterAst componentParameterAst = mock(ComponentParameterAst.class);
    when(componentParameterAst.getResolvedRawValue()).thenReturn(TEST_STRING_RAW_VALUE);

    when(componentModel.getParameter("value")).thenReturn(componentParameterAst);
    when(createBeanDefinitionRequest.getComponentModel()).thenReturn(componentModel);

    mockComponentBuildingDefinition(createBeanDefinitionRequest, Optional.empty());

    // When
    simpleTypeBeanDefinitionCreator.handleRequest(springComponentModels, createBeanDefinitionRequest);

    // Then
    ArgumentCaptor<BeanDefinition> beanDefinitionArgumentCaptor = ArgumentCaptor.forClass(BeanDefinition.class);
    verify(springComponentModel, times(1)).setBeanDefinition(beanDefinitionArgumentCaptor.capture());
    BeanDefinition value = beanDefinitionArgumentCaptor.getValue();
    assertThat(value.getBeanClassName(), is(String.class.getName()));
    assertThat(value.getConstructorArgumentValues().getArgumentValue(0, String.class).getValue(), is(TEST_STRING_RAW_VALUE));
  }

  @Test
  public void testHandleRequestCallsSetBeanDefinitionWithResultGetConvertibleBeanDefinition_WhenCreateBeanDefinitionRequestTypeIsSimpleTypeAndParamNotFoundInOwnerComponentButFoundInComponentModelAndTypeConverter() {
    // Given
    mockStringObjectTypeVisitor(createBeanDefinitionRequest, String.class);

    SpringComponentModel springComponentModel = mock(SpringComponentModel.class);
    when(createBeanDefinitionRequest.getSpringComponentModel()).thenReturn(springComponentModel);

    when(parameterUtils.getParamInOwnerComponent(createBeanDefinitionRequest)).thenReturn(null);

    ComponentAst componentModel = mock(ComponentAst.class);
    ComponentParameterAst componentParameterAst = mock(ComponentParameterAst.class);
    when(componentParameterAst.getResolvedRawValue()).thenReturn(TEST_STRING_RAW_VALUE);

    when(componentModel.getParameter("value")).thenReturn(componentParameterAst);
    when(createBeanDefinitionRequest.getComponentModel()).thenReturn(componentModel);

    mockComponentBuildingDefinition(createBeanDefinitionRequest, Optional.of(o -> TEST_STRING_RAW_VALUE));

    // When
    simpleTypeBeanDefinitionCreator.handleRequest(springComponentModels, createBeanDefinitionRequest);

    // Then
    ArgumentCaptor<BeanDefinition> beanDefinitionArgumentCaptor = ArgumentCaptor.forClass(BeanDefinition.class);
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

  private void mockStringObjectTypeVisitor(CreateBeanDefinitionRequest createBeanDefinitionRequest, Class<?> aClass) {
    ObjectTypeVisitor objectTypeVisitor = mock(ObjectTypeVisitor.class);
    when(objectTypeVisitor.getType()).thenAnswer((Answer<Class<?>>) invocationOnMock -> aClass);
    when(createBeanDefinitionRequest.retrieveTypeVisitor()).thenReturn(objectTypeVisitor);
  }

  private static class MyComponentParameterAst implements ComponentParameterAst {

    @Override
    public ParameterModel getModel() {
      return null;
    }

    @Override
    public <T> Either<String, T> getValue() {
      return null;
    }

    @Override
    public String getRawValue() {
      return null;
    }

    @Override
    public String getResolvedRawValue() {
      return TEST_STRING_RAW_VALUE;
    }

    @Override
    public Optional<ComponentMetadataAst> getMetadata() {
      return Optional.empty();
    }

    @Override
    public ComponentGenerationInformation getGenerationInformation() {
      return null;
    }

    @Override
    public boolean isDefaultValue() {
      return false;
    }
  }
}
