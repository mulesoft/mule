/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

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
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.JavaTypeLoader;
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class JavaValueProviderModelValidatorTestCase {

  private final JavaTypeLoader loader = new JavaTypeLoader(this.getClass().getClassLoader());
  private final MetadataType STRING_TYPE = loader.load(String.class);
  private final MetadataType NUMBER_TYPE = loader.load(Integer.class);
  private final MetadataType OBJECT_TYPE = loader.load(InputStream.class);
  private JavaValueProviderModelValidator valueProviderModelValidator;

  private ProblemsReporter problemsReporter;

  @Mock(lenient = true)
  ExtensionModel extensionModel;

  @Mock(lenient = true)
  OperationModel operationModel;

  @Mock(lenient = true)
  ParameterModel operationParameter;

  @Mock(lenient = true)
  ParameterModel configrationParameter;

  @Mock(lenient = true)
  ConfigurationModel configurationModel;

  @Mock(lenient = true)
  ParameterGroupModel parameterGroupModel;

  @Mock(lenient = true)
  ParameterGroupModel configurationParameterGroupModel;

  private ValueProviderFactoryModelPropertyBuilder operationParameterBuilder;
  private ValueProviderFactoryModelPropertyBuilder configrationParameterBuilder;

  @Before
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
    when(operationModel.getAllParameterModels()).thenReturn(asList(operationParameter));
    when(operationModel.getName()).thenReturn("superOperation");
    when(parameterGroupModel.getParameterModels()).thenReturn(asList(operationParameter));

    when(operationModel.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));
    mockParameter(configrationParameter, configrationParameterBuilder);
    mockParameter(operationParameter, operationParameterBuilder);
  }

  @Test
  public void valueProviderShouldBeInstantiable() {
    ValueProviderFactoryModelPropertyBuilder builder =
        ValueProviderFactoryModelProperty.builder(NonInstantiableProvider.class);
    mockParameter(operationParameter, builder, "anotherId");

    validate();
    assertProblems("The Value Provider [NonInstantiableProvider] is not instantiable but it should");
  }

  @Test
  public void parameterShouldExist() {
    operationParameterBuilder.withInjectableParameter("someParam", STRING_TYPE, true);
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(operationParameterBuilder.build()));

    validate();
    assertProblems("The Value Provider [SomeValueProvider] declares to use a parameter 'someParam' which doesn't exist in the operation 'superOperation'");
  }

  @Test
  public void parameterShouldBeOfSametype() {
    operationParameterBuilder.withInjectableParameter("someName", NUMBER_TYPE, true);
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(operationParameterBuilder.build()));

    validate();
    assertProblems("The Value Provider [SomeValueProvider] defines a parameter 'someName' of type 'class java.lang.Integer' but in the operation 'superOperation' is of type 'class java.lang.String'");
  }

  @Test
  public void injectConnectionInConnectionLessComponent() throws NoSuchFieldException {
    operationParameterBuilder.withConnection(SomeValueProvider.class.getDeclaredField("connection"));
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(operationParameterBuilder.build()));

    validate();
    assertProblems("The Value Provider [SomeValueProvider] defines that requires a connection, but is used in the operation 'superOperation' which is connection less");
  }

  @Test
  public void configurationBasedValueProviderDoesntSupportConnectionInjection() throws NoSuchFieldException {
    configrationParameterBuilder.withConnection(SomeValueProvider.class.getDeclaredField("connection"));
    mockParameter(configrationParameter, configrationParameterBuilder);
    when(configurationModel.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(configrationParameterBuilder.build()));

    validate();
    assertProblems("The Value Provider [SomeValueProvider] defines that requires a connection which is not allowed for a Value Provider of a configuration's parameter [SomeConfig]");
  }

  @Test
  public void configurationBasedValueProviderDoesntSupportConfigurationInjection() throws NoSuchFieldException {
    configrationParameterBuilder.withConfig(SomeValueProvider.class.getDeclaredField("connection"));
    mockParameter(configrationParameter, configrationParameterBuilder);
    when(configurationModel.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(configrationParameterBuilder.build()));

    validate();
    assertProblems("The Value Provider [SomeValueProvider] defines that requires a configuration which is not allowed for a Value Provider of a configuration's parameter [SomeConfig]");
  }

  @Test
  public void parameterWithValueProviderShouldBeOfStringType() {
    when(operationParameter.getType()).thenReturn(NUMBER_TYPE);

    validate();
    assertProblems("The parameter [someName] of the operation 'superOperation' is not of String type. Parameters that provides Values should be of String type.");
  }

  @Test
  public void parameterWithValueProviderHasRepeatedIdInCompileTime() {
    ValueProviderFactoryModelPropertyBuilder builder =
        ValueProviderFactoryModelProperty.builder(SomeOtherValueProvider.class);
    mockParameter(operationParameter, builder);

    validate();
    assertProblems("The following ValueProvider implementations [org.mule.runtime.module.extension.internal.loader.validation.JavaValueProviderModelValidatorTestCase$SomeValueProvider, org.mule.runtime.module.extension.internal.loader.validation.JavaValueProviderModelValidatorTestCase$SomeOtherValueProvider] use the same id [valueProviderId]. ValueProvider ids must be unique.");
  }

  @Test
  public void boundParameterExists() {
    operationParameterBuilder.withInjectableParameter("actingParameter", STRING_TYPE, true, "someName");
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(operationParameterBuilder.build()));

    validate();
    assertNoErrors();
  }

  @Test
  public void boundParameterShouldExist() {
    operationParameterBuilder.withInjectableParameter("actingParameter", STRING_TYPE, true, "anotherName");
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(operationParameterBuilder.build()));

    validate();
    assertProblems("The Value Provider [SomeValueProvider] declares to use a parameter 'anotherName' which doesn't exist in the operation 'superOperation'");
  }

  @Test
  public void boundParameterFromExtractionExpressionExists() {
    operationParameterBuilder.withInjectableParameter("actingParameter", STRING_TYPE, true, "someName.someTag.@attribute");
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(operationParameterBuilder.build()));

    validate();
    assertNoErrors();
  }

  @Test
  public void boundParameterFromExtractionExpressionShouldExist() {
    operationParameterBuilder.withInjectableParameter("actingParameter", STRING_TYPE, true, "anotherName.nested.fields");
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class))
        .thenReturn(Optional.of(operationParameterBuilder.build()));

    validate();
    assertProblems("The Value Provider [SomeValueProvider] declares to use a parameter 'anotherName' which doesn't exist in the operation 'superOperation'");
  }

  @Test
  public void parameterWithValueProviderHasDifferentIdInCompileTime() {
    ValueProviderFactoryModelPropertyBuilder builder =
        ValueProviderFactoryModelProperty.builder(SomeOtherValueProvider.class);
    mockParameter(operationParameter, builder, "anotherId");

    validate();
    assertNoErrors();
  }

  @Test
  public void parameterShouldNotBeAnnotatedWithBothOfValuesAndFieldValues() {
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
  public void parameterWithFieldValueProviderDoNotHaveToBeStringType() {
    when(operationParameter.getModelProperty(ValueProviderFactoryModelProperty.class)).thenReturn(empty());

    ValueProviderFactoryModelPropertyBuilder valueProviderFactoryModelPropertyBuilder =
        ValueProviderFactoryModelProperty.builder(SomeValueProvider.class);
    Map<String, ValueProviderFactoryModelPropertyBuilder> valueProviderFactoryModelPropertyBuilders =
        singletonMap("simple.path", valueProviderFactoryModelPropertyBuilder);

    mockParameter(operationParameter, valueProviderFactoryModelPropertyBuilders);

    validate();
    assertNoErrors();
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
    when(parameter.getModelProperty(ValueProviderFactoryModelProperty.class)).thenReturn(Optional.of(builder.build()));
    when(parameter.getModelProperty(ImplementingParameterModelProperty.class)).thenReturn(empty());
    when(parameter.getModelProperty(DeclaringMemberModelProperty.class)).thenReturn(empty());
    when(parameter.getName()).thenReturn("someName");
    when(parameter.getType()).thenReturn(STRING_TYPE);
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
}
