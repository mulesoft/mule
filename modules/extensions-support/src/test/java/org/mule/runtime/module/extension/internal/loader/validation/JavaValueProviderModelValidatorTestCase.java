/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.visitableMock;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.annotation.TypeAnnotation;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.impl.DefaultArrayType;
import org.mule.metadata.api.model.impl.DefaultNumberType;
import org.mule.metadata.api.model.impl.DefaultObjectType;
import org.mule.metadata.api.model.impl.DefaultStringType;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.FieldValueProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.FieldsValueProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ValueProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ValueProviderFactoryModelProperty.ValueProviderFactoryModelPropertyBuilder;
import org.mule.runtime.module.extension.internal.loader.java.validation.JavaValueProviderModelValidator;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@SmallTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class JavaValueProviderModelValidatorTestCase {

  private final JavaTypeLoader loader = new JavaTypeLoader(this.getClass().getClassLoader());
  private final MetadataType STRING_TYPE = loader.load(String.class);
  private final MetadataType NUMBER_TYPE = loader.load(Integer.class);
  private final MetadataType OBJECT_TYPE = loader.load(InputStream.class);
  private final MetadataType STRING_TYPE_WITH_ANNOTATIONS = new DefaultStringType(MetadataFormat.JAVA, createStringAnnotations());
  private final MetadataType NUMBER_TYPE_WITH_ANNOTATIONS = new DefaultNumberType(MetadataFormat.JAVA, createNumberAnnotations());
  private final MetadataType OBJECT_TYPE_WITH_ANNOTATIONS =
      new DefaultObjectType(emptyList(), false, null, MetadataFormat.JSON, createObjectAnnotations());
  private final MetadataType ARRAY_TYPE_WITH_ANNOTATIONS =
      new DefaultArrayType(() -> STRING_TYPE, MetadataFormat.JAVA, createObjectAnnotations());
  private JavaValueProviderModelValidator valueProviderModelValidator;

  private ProblemsReporter problemsReporter;

  @Mock
  ExtensionModel extensionModel;

  @Mock
  OperationModel operationModel;

  @Mock
  ParameterModel operationParameter;

  @Mock
  ParameterModel anotherOperationParameter;

  @Mock
  ParameterModel configrationParameter;

  @Mock
  ConfigurationModel configurationModel;

  @Mock
  ParameterGroupModel parameterGroupModel;

  @Mock
  ParameterGroupModel configurationParameterGroupModel;

  private ValueProviderFactoryModelPropertyBuilder operationParameterBuilder;
  private ValueProviderFactoryModelPropertyBuilder configrationParameterBuilder;

  @BeforeEach
  public void setUp() {
    valueProviderModelValidator = new JavaValueProviderModelValidator();
    problemsReporter = new ProblemsReporter(extensionModel);

    operationParameterBuilder = ValueProviderFactoryModelProperty.builder(SomeValueProvider.class);
    configrationParameterBuilder = ValueProviderFactoryModelProperty.builder(SomeValueProvider.class);

    visitableMock(operationModel);

    when(extensionModel.getConfigurationModels()).thenReturn(asList(configurationModel));
    when(configurationModel.getAllParameterModels()).thenReturn(asList(configrationParameter));
    when(configurationModel.getParameterGroupModels()).thenReturn(asList(configurationParameterGroupModel));
    when(configurationModel.getName()).thenReturn("SomeConfig");
    when(configurationParameterGroupModel.getParameterModels()).thenReturn(asList(configrationParameter));

    when(extensionModel.getOperationModels()).thenReturn(singletonList(operationModel));
    when(operationModel.getAllParameterModels()).thenReturn(asList(operationParameter, anotherOperationParameter));
    when(operationModel.getName()).thenReturn("superOperation");
    when(parameterGroupModel.getParameterModels()).thenReturn(asList(operationParameter, anotherOperationParameter));

    when(operationModel.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));
    mockParameter(configrationParameter, configrationParameterBuilder);
    mockParameter(operationParameter, operationParameterBuilder);
    mockAnotherParameter(anotherOperationParameter, STRING_TYPE);
  }

  @Test
  void valueProviderShouldBeInstantiable() {
    ValueProviderFactoryModelPropertyBuilder builder =
        ValueProviderFactoryModelProperty.builder(NonInstantiableProvider.class);
    mockParameter(operationParameter, builder, "anotherId");

    validate();
    assertProblems("The Value Provider [NonInstantiableProvider] is not instantiable but it should");
  }

  @Test
  void parameterShouldExist() {
    operationParameterBuilder.withInjectableParameter("someParam", STRING_TYPE, true);
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(operationParameterBuilder.build()));

    validate();
    assertProblems("The Value Provider [SomeValueProvider] declares to use a parameter 'someParam' which doesn't exist in the operation 'superOperation'");
  }

  @Test
  void parameterShouldBeOfSametype() {
    operationParameterBuilder.withInjectableParameter("someName", NUMBER_TYPE, true);
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(operationParameterBuilder.build()));

    validate();
    assertProblems("The Value Provider [SomeValueProvider] defines a parameter 'someName' of type 'class java.lang.Integer' but in the operation 'superOperation' is of type 'class java.lang.String'");
  }

  @Test
  void injectConnectionInConnectionLessComponent() throws NoSuchFieldException {
    operationParameterBuilder.withConnection(SomeValueProvider.class.getDeclaredField("connection"));
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(operationParameterBuilder.build()));

    validate();
    assertProblems("The Value Provider [SomeValueProvider] defines that requires a connection, but is used in the operation 'superOperation' which is connection less");
  }

  @Test
  void configurationBasedValueProviderDoesntSupportConnectionInjection() throws NoSuchFieldException {
    configrationParameterBuilder.withConnection(SomeValueProvider.class.getDeclaredField("connection"));
    mockParameter(configrationParameter, configrationParameterBuilder);
    when(configurationModel.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(configrationParameterBuilder.build()));

    validate();
    assertProblems("The Value Provider [SomeValueProvider] defines that requires a connection which is not allowed for a Value Provider of a configuration's parameter [SomeConfig]");
  }

  @Test
  void configurationBasedValueProviderDoesntSupportConfigurationInjection() throws NoSuchFieldException {
    configrationParameterBuilder.withConfig(SomeValueProvider.class.getDeclaredField("connection"));
    mockParameter(configrationParameter, configrationParameterBuilder);
    when(configurationModel.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(configrationParameterBuilder.build()));

    validate();
    assertProblems("The Value Provider [SomeValueProvider] defines that requires a configuration which is not allowed for a Value Provider of a configuration's parameter [SomeConfig]");
  }

  @Test
  void parameterWithValueProviderShouldBeOfStringType() {
    when(operationParameter.getType()).thenReturn(NUMBER_TYPE);

    validate();
    assertProblems("The parameter [someName] of the operation 'superOperation' is not of String type. Parameters that provides Values should be of String type.");
  }

  @Test
  void parameterWithValueProviderHasRepeatedIdInCompileTime() {
    ValueProviderFactoryModelPropertyBuilder builder =
        ValueProviderFactoryModelProperty.builder(SomeOtherValueProvider.class);
    mockParameter(operationParameter, builder);

    validate();
    assertProblems("The following ValueProvider implementations [org.mule.runtime.module.extension.internal.loader.validation.JavaValueProviderModelValidatorTestCase$SomeValueProvider, org.mule.runtime.module.extension.internal.loader.validation.JavaValueProviderModelValidatorTestCase$SomeOtherValueProvider] use the same id [valueProviderId]. ValueProvider ids must be unique.");
  }

  @Test
  void boundParameterExists() {
    operationParameterBuilder.withInjectableParameter("actingParameter", STRING_TYPE, true, "someName");
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(operationParameterBuilder.build()));

    validate();
    assertNoErrors();
  }

  @Test
  void boundParameterShouldExist() {
    operationParameterBuilder.withInjectableParameter("actingParameter", STRING_TYPE, true, "anotherName");
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(operationParameterBuilder.build()));

    validate();
    assertProblems("The Value Provider [SomeValueProvider] declares to use a parameter 'anotherName' which doesn't exist in the operation 'superOperation'");
  }

  @Test
  void boundParameterFromExtractionExpressionExists() {
    operationParameterBuilder.withInjectableParameter("actingParameter", STRING_TYPE, true, "someName.someTag.@attribute");
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(operationParameterBuilder.build()));

    validate();
    assertNoErrors();
  }

  @Test
  void boundParameterFromExtractionExpressionShouldExist() {
    operationParameterBuilder.withInjectableParameter("actingParameter", STRING_TYPE, true, "anotherName.nested.fields");
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(operationParameterBuilder.build()));

    validate();
    assertProblems("The Value Provider [SomeValueProvider] declares to use a parameter 'anotherName' which doesn't exist in the operation 'superOperation'");
  }

  @Test
  void parameterWithValueProviderHasDifferentIdInCompileTime() {
    ValueProviderFactoryModelPropertyBuilder builder =
        ValueProviderFactoryModelProperty.builder(SomeOtherValueProvider.class);
    mockParameter(operationParameter, builder, "anotherId");

    validate();
    assertNoErrors();
  }

  @Test
  void parameterShouldNotBeAnnotatedWithBothOfValuesAndFieldValues() {
    ValueProviderFactoryModelPropertyBuilder builder =
        ValueProviderFactoryModelProperty.builder(SomeValueProvider.class);
    mockParameter(operationParameter, builder);

    ValueProviderFactoryModelPropertyBuilder otherBuilder =
        ValueProviderFactoryModelProperty.builder(SomeOtherValueProvider.class);
    Map<String, ValueProviderFactoryModelProperty> fieldsValueProviderFactories =
        singletonMap("simple.path", otherBuilder.build());
    FieldsValueProviderFactoryModelProperty fieldsValueProviderFactoryModelProperty =
        new FieldsValueProviderFactoryModelProperty(fieldsValueProviderFactories);

    when(operationParameter.getModelProperty(FieldsValueProviderFactoryModelProperty.class))
        .thenReturn(of(fieldsValueProviderFactoryModelProperty));

    validate();
    assertProblems("Parameter [someName] from operation with name superOperation has both a Value Provider and a Field Value Provider");
  }

  @Test
  void parameterWithFieldValueProviderDoNotHaveToBeStringType() {
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class)).thenReturn(empty());

    ValueProviderFactoryModelPropertyBuilder valueProviderFactoryModelPropertyBuilder =
        ValueProviderFactoryModelProperty.builder(SomeValueProvider.class);
    Map<String, ValueProviderFactoryModelPropertyBuilder> valueProviderFactoryModelPropertyBuilders =
        singletonMap("simple.path", valueProviderFactoryModelPropertyBuilder);

    mockParameter(operationParameter, valueProviderFactoryModelPropertyBuilders);

    validate();
    assertNoErrors();
  }

  @Test
  void modelTypeMatchesValueProviderTypeForStringParameter() {
    mockAnotherParameter(anotherOperationParameter, STRING_TYPE_WITH_ANNOTATIONS);
    operationParameterBuilder.withInjectableParameter("anotherParameter", OBJECT_TYPE, true);
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(operationParameterBuilder.build()));
    validate();
    assertNoErrors();
  }

  @Test
  void modelTypeMatchesValueProviderTypeForNumberParameter() {
    mockAnotherParameter(anotherOperationParameter, NUMBER_TYPE_WITH_ANNOTATIONS);
    operationParameterBuilder.withInjectableParameter("anotherParameter", OBJECT_TYPE, true);
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(operationParameterBuilder.build()));
    validate();
    assertNoErrors();
  }

  @Test
  void modelTypeMatchesValueProviderTypeForObjectParameter() {
    mockAnotherParameter(anotherOperationParameter, OBJECT_TYPE_WITH_ANNOTATIONS);
    operationParameterBuilder.withInjectableParameter("anotherParameter", OBJECT_TYPE, true);
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(operationParameterBuilder.build()));
    validate();
    assertNoErrors();
  }

  @Test
  void modelTypeMatchesValueProviderTypeForArrayParameter() {
    mockAnotherParameter(anotherOperationParameter, ARRAY_TYPE_WITH_ANNOTATIONS);
    operationParameterBuilder.withInjectableParameter("anotherParameter", OBJECT_TYPE, true);
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(operationParameterBuilder.build()));
    validate();
    assertNoErrors();
  }

  @Test
  void modelTypeContainsTwoParametersWithTheSameName() {
    ParameterModel clashingParameterMock = mock(ParameterModel.class);

    mockParameter(clashingParameterMock, operationParameterBuilder, "anotherValueProviderId", "anotherParameter", OBJECT_TYPE);
    when(clashingParameterMock.getModelProperty(ValueProviderFactoryModelProperty.class)).thenReturn(empty());
    when(operationModel.getAllParameterModels())
        .thenReturn(asList(operationParameter, anotherOperationParameter, clashingParameterMock));
    when(parameterGroupModel.getParameterModels())
        .thenReturn(asList(operationParameter, anotherOperationParameter, clashingParameterMock));

    validate();
    assertProblems("Parameter [someName] from operation with name superOperation has a Value Provider defined, but that operation has one or more parameters with repeated names [anotherParameter]. Components with parameters with non-unique names do not support Value Providers");
  }

  private void assertProblems(String errorMessage) {
    List<Problem> errors = problemsReporter.getErrors();
    assertThat(errors, hasSize(1));
    assertThat(errors.get(0).getMessage(), is(errorMessage));
  }

  private void assertNoErrors() {
    List<Problem> errors = problemsReporter.getErrors();
    assertThat(errors, hasSize(0));
  }

  private void validate() {
    valueProviderModelValidator.validate(extensionModel, problemsReporter);
  }

  private void mockParameter(ParameterModel parameter, ValueProviderFactoryModelPropertyBuilder builder) {
    mockParameter(parameter, builder, "valueProviderId");
  }

  private void mockParameter(ParameterModel parameter, ValueProviderFactoryModelPropertyBuilder builder, String valueProviderId) {
    mockParameter(parameter, builder, valueProviderId, "someName", STRING_TYPE);
  }

  private void mockParameter(ParameterModel parameter, ValueProviderFactoryModelPropertyBuilder builder, String valueProviderId,
                             String paramName, MetadataType type) {
    when(parameter.getModelProperty(ValueProviderFactoryModelProperty.class)).thenReturn(Optional.of(builder.build()));
    when(parameter.getModelProperty(ImplementingParameterModelProperty.class)).thenReturn(empty());
    when(parameter.getModelProperty(DeclaringMemberModelProperty.class)).thenReturn(empty());
    when(parameter.getName()).thenReturn(paramName);
    when(parameter.getType()).thenReturn(type);
    when(parameter.getValueProviderModel())
        .thenReturn(of(new ValueProviderModel(emptyList(), false, false, true, 1, "name", valueProviderId)));
  }

  private void mockParameter(ParameterModel parameter,
                             Map<String, ValueProviderFactoryModelPropertyBuilder> valueProviderFactoryModelPropertyBuilders) {

    Map<String, ValueProviderFactoryModelProperty> fieldsValueProviderFactories = new HashMap<>();
    valueProviderFactoryModelPropertyBuilders.forEach((targetSelector, valueProviderFactoryModelPropertyBuilder) -> {
      fieldsValueProviderFactories.put(targetSelector, valueProviderFactoryModelPropertyBuilder.build());
    });
    FieldsValueProviderFactoryModelProperty fieldsValueProviderFactoryModelProperty =
        new FieldsValueProviderFactoryModelProperty(fieldsValueProviderFactories);

    FieldValueProviderModel fieldValueProviderModel =
        new FieldValueProviderModel(emptyList(), false, false, true, 1, "name", "providerId", "simple.path");
    List<FieldValueProviderModel> fieldValueProviderModels = singletonList(fieldValueProviderModel);

    when(operationParameter.getModelProperty(FieldsValueProviderFactoryModelProperty.class))
        .thenReturn(of(fieldsValueProviderFactoryModelProperty));
    when(parameter.getModelProperty(ImplementingParameterModelProperty.class)).thenReturn(empty());
    when(parameter.getModelProperty(DeclaringMemberModelProperty.class)).thenReturn(empty());
    when(parameter.getName()).thenReturn("someName");
    when(parameter.getType()).thenReturn(OBJECT_TYPE);
    when(parameter.getFieldValueProviderModels()).thenReturn(fieldValueProviderModels);
  }

  private void mockAnotherParameter(ParameterModel parameter, MetadataType type) {
    when(parameter.getModelProperty(ImplementingParameterModelProperty.class)).thenReturn(empty());
    when(parameter.getModelProperty(DeclaringMemberModelProperty.class)).thenReturn(empty());
    when(parameter.getName()).thenReturn("anotherParameter");
    when(parameter.getType()).thenReturn(type);
  }

  public static class SomeValueProvider implements ValueProvider {

    @Connection
    String connection;

    @Override
    public Set<Value> resolve() {
      return emptySet();
    }
  }

  public static class SomeOtherValueProvider implements ValueProvider {

    @Connection
    String connection;

    @Override
    public Set<Value> resolve() {
      return emptySet();
    }
  }

  public class NonInstantiableProvider implements ValueProvider {

    private NonInstantiableProvider() {}

    @Override
    public Set<Value> resolve() throws ValueResolvingException {
      return null;
    }
  }

  private Map<Class<? extends TypeAnnotation>, TypeAnnotation> createStringAnnotations() {
    Map<Class<? extends TypeAnnotation>, TypeAnnotation> annotations = new HashMap<>();
    annotations.put(ClassInformationAnnotation.class, new ClassInformationAnnotation(InputStream.class));
    annotations.put(EnumAnnotation.class, new EnumAnnotation<>(new String[] {"value1", "value2"}));
    return annotations;
  }

  private Map<Class<? extends TypeAnnotation>, TypeAnnotation> createNumberAnnotations() {
    Map<Class<? extends TypeAnnotation>, TypeAnnotation> annotations = new HashMap<>();
    annotations.put(ClassInformationAnnotation.class, new ClassInformationAnnotation(InputStream.class));
    annotations.put(EnumAnnotation.class, new EnumAnnotation<>(new Number[] {1, 2}));
    return annotations;
  }

  private Map<Class<? extends TypeAnnotation>, TypeAnnotation> createObjectAnnotations() {
    Map<Class<? extends TypeAnnotation>, TypeAnnotation> annotations = new HashMap<>();
    annotations.put(ClassInformationAnnotation.class, new ClassInformationAnnotation(InputStream.class));
    return annotations;
  }
}
