/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.runtime.core.api.util.ClassUtils.getMethod;
import static org.mule.runtime.module.extension.internal.loader.java.property.SampleDataProviderFactoryModelProperty.builder;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.model.ConnectableComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.data.sample.SampleDataProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.SampleDataProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.SampleDataProviderFactoryModelProperty.SampleDataProviderFactoryModelPropertyBuilder;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.sdk.api.annotation.param.Config;
import org.mule.sdk.api.annotation.param.Connection;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.data.sample.SampleDataProvider;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.streaming.PagingProvider;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class SampleDataProviderModelValidatorTestCase {

  private static Result<String, String> OUTPUT_RESULT;
  private final JavaTypeLoader loader = new JavaTypeLoader(this.getClass().getClassLoader());
  private final MetadataType STRING_TYPE = loader.load(String.class);
  private final MetadataType NUMBER_TYPE = loader.load(Integer.class);
  private final ReflectionCache reflectionCache = new ReflectionCache();

  private SampleDataModelValidator validator;
  private ProblemsReporter problemsReporter;

  @Mock(lenient = true)
  private ExtensionModel extensionModel;

  @Mock(lenient = true, answer = RETURNS_DEEP_STUBS)
  private OperationModel operationModel;

  @Mock(lenient = true)
  private ParameterModel parameterModel;

  @Mock(lenient = true)
  private ConfigurationModel configurationModel;

  @Mock(lenient = true)
  private ParameterGroupModel parameterGroupModel;

  @Mock(lenient = true)
  private ParameterGroupModel configurationParameterGroupModel;

  @Mock(lenient = true, answer = RETURNS_DEEP_STUBS)
  private SourceModel sourceModel;

  private SampleDataProviderFactoryModelPropertyBuilder providerBuilder;

  private ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Before
  public void setUp() {
    OUTPUT_RESULT = mock(Result.class);
    validator = new SampleDataModelValidator();
    problemsReporter = new ProblemsReporter(extensionModel);

    providerBuilder = builder(ConnectedSampleDataProvider.class);

    visitableMock(operationModel);

    when(extensionModel.getConfigurationModels()).thenReturn(asList(configurationModel));
    when(configurationModel.getName()).thenReturn("SomeConfig");
    when(configurationModel.getOperationModels()).thenReturn(emptyList());
    when(configurationModel.getSourceModels()).thenReturn(asList(sourceModel));
    when(configurationParameterGroupModel.getParameterModels()).thenReturn(emptyList());

    when(parameterGroupModel.getParameterModels()).thenReturn(asList(parameterModel));

    when(extensionModel.getOperationModels()).thenReturn(singletonList(operationModel));
    when(operationModel.getName()).thenReturn("myOp");
    when(operationModel.getAllParameterModels()).thenReturn(asList(parameterModel));
    when(operationModel.getName()).thenReturn("superOperation");
    when(operationModel.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));
    when(operationModel.requiresConnection()).thenReturn(true);

    when(sourceModel.getName()).thenReturn("listener");
    when(sourceModel.getParameterGroupModels()).thenReturn(asList(parameterGroupModel));

    when(parameterModel.getModelProperty(ImplementingParameterModelProperty.class)).thenReturn(empty());
    when(parameterModel.getModelProperty(DeclaringMemberModelProperty.class)).thenReturn(empty());
    when(parameterModel.getName()).thenReturn("someName");
    when(parameterModel.getType()).thenReturn(STRING_TYPE);

    mockOutput(operationModel);
    mockOutput(sourceModel);
  }

  private void mockOutput(ConnectableComponentModel model) {
    when(model.getOutput().getType()).thenReturn(typeLoader.load(String.class));
    when(model.getOutputAttributes().getType()).thenReturn(typeLoader.load(String.class));
  }

  private void mockOperationProvider() {
    mockComponent(operationModel, providerBuilder, ConnectedSampleDataProvider.class.getSimpleName());
  }

  private void mockSourceProvider() {
    mockComponent(sourceModel, builder(ConnectedSampleDataProvider.class), ConfigAwareSampleDataProvider.class.getSimpleName());
  }

  @After
  public void tearDown() {
    OUTPUT_RESULT = null;
  }

  @Test
  public void providerShouldBeInstantiable() {
    SampleDataProviderFactoryModelPropertyBuilder builder = builder(NonInstantiableProvider.class);
    mockComponent(operationModel, builder, "anotherId");

    validate();
    assertProblems("The SampleDataProvider [NonInstantiableProvider] is not instantiable");
  }

  @Test
  public void parameterShouldExist() {
    providerBuilder.withInjectableParameter("someParam", STRING_TYPE, true);
    when(operationModel.getModelProperty(SampleDataProviderFactoryModelProperty.class))
        .thenReturn(of(providerBuilder.build()));

    mockOperationProvider();

    validate();
    assertProblems("The SampleDataProvider [ConnectedSampleDataProvider] declares a parameter 'someParam' which doesn't exist in the operation 'superOperation'");
  }

  @Test
  public void parameterShouldBeOfSameType() {
    providerBuilder.withInjectableParameter("someName", NUMBER_TYPE, true);
    mockOperationProvider();

    validate();
    assertProblems("The SampleDataProvider [ConnectedSampleDataProvider] defines a parameter 'someName' of type 'class java.lang.Integer' but in the operation 'superOperation' is of type 'class java.lang.String'");
  }

  @Test
  public void injectConnectionInConnectionLessComponent() {
    providerBuilder.withConnection(getField(ConnectedSampleDataProvider.class, "connection", reflectionCache).get());
    when(operationModel.requiresConnection()).thenReturn(false);
    mockOperationProvider();

    validate();
    assertProblems("The SampleDataProvider [ConnectedSampleDataProvider] defines that it requires a connection, but is used in the operation 'superOperation' which is connection less");
  }

  @Test
  public void injectConfigInConfigLessComponent() {
    providerBuilder.withConfig(getField(ConfigAwareSampleDataProvider.class, "config", reflectionCache).get());
    mockOperationProvider();

    validate();
    assertProblems("The SampleDataProvider [ConnectedSampleDataProvider] defines that it requires a config, but is used in the operation 'superOperation' which is config less");
  }

  @Test
  public void injectConfigInConfigAwareComponent() {
    providerBuilder.withConfig(getField(ConfigAwareSampleDataProvider.class, "config", reflectionCache).get());
    when(configurationModel.getOperationModels()).thenReturn(asList(operationModel));
    when(extensionModel.getOperationModels()).thenReturn(emptyList());
    mockOperationProvider();

    validate();
  }

  @Test
  public void duplicateProviderId() {
    mockOperationProvider();
    String id = ConnectedSampleDataProvider.class.getSimpleName();
    mockComponent(sourceModel, builder(ConfigAwareSampleDataProvider.class), id);

    validate();
    assertProblems(format("The following SampleDataProvider implementations [%s, %s] use the same id [%s]. SampleDataProvider ids must be unique.",
                          ConfigAwareSampleDataProvider.class.getName(), ConnectedSampleDataProvider.class.getName(), id));
  }

  @Test
  public void legalComponent() {
    mockOperationProvider();
    mockSourceProvider();

    validate();
    assertNoErrors();
  }

  @Test
  public void operationWithWrongPayloadTypeSampleDataProvider() {
    assertWrongGenerics(operationModel,
                        MapSampleDataProvider.class,
                        "SampleDataProvider [org.mule.runtime.module.extension.internal.loader.validation.SampleDataProviderModelValidatorTestCase$MapSampleDataProvider] is used at component 'superOperation' which outputs a Result<java.lang.String,java.lang.String>, but the provider generic signature is '<java.util.Map<java.lang.String,java.lang.String>,java.lang.String>'");
  }

  @Test
  public void operationWithWrongAttributesTypeSampleDataProvider() {
    assertWrongGenerics(operationModel,
                        MapAttributesSampleDataProvider.class,
                        "SampleDataProvider [org.mule.runtime.module.extension.internal.loader.validation.SampleDataProviderModelValidatorTestCase$MapAttributesSampleDataProvider] is used at component 'superOperation' which outputs a Result<java.lang.String,java.lang.String>, but the provider generic signature is '<java.lang.String,java.util.Map<java.lang.String,java.lang.String>>'");
  }

  @Test
  public void sourceWithWrongPayloadTypeSampleDataProvider() {
    assertWrongGenerics(sourceModel,
                        MapSampleDataProvider.class,
                        "SampleDataProvider [org.mule.runtime.module.extension.internal.loader.validation.SampleDataProviderModelValidatorTestCase$MapSampleDataProvider] is used at component 'listener' which outputs a Result<java.lang.String,java.lang.String>, but the provider generic signature is '<java.util.Map<java.lang.String,java.lang.String>,java.lang.String>'");
  }

  @Test
  public void sourceWithWrongAttributesTypeSampleDataProvider() {
    assertWrongGenerics(sourceModel,
                        MapAttributesSampleDataProvider.class,
                        "SampleDataProvider [org.mule.runtime.module.extension.internal.loader.validation.SampleDataProviderModelValidatorTestCase$MapAttributesSampleDataProvider] is used at component 'listener' which outputs a Result<java.lang.String,java.lang.String>, but the provider generic signature is '<java.lang.String,java.util.Map<java.lang.String,java.lang.String>>'");
  }

  @Test
  public void operationWithBoxedVoidAttributes() {
    mockComponent(operationModel, builder(VoidAttributesSampleDataProvider.class),
                  VoidAttributesSampleDataProvider.class.getSimpleName());

    when(operationModel.getOutputAttributes().getType()).thenReturn(typeLoader.load(Void.class));

    validate();
    assertNoErrors();
  }

  @Test
  public void operationWithNativeVoidAttributes() {
    mockComponent(operationModel, builder(VoidAttributesSampleDataProvider.class),
                  VoidAttributesSampleDataProvider.class.getSimpleName());

    when(operationModel.getOutputAttributes().getType()).thenReturn(typeLoader.load(void.class));

    validate();
    assertNoErrors();
  }

  @Test
  public void pagingOperationWithInputStreamPayload() {
    MetadataType type = create(JAVA).arrayType()
            .with(new ClassInformationAnnotation(PagingProvider.class, asList(Object.class, InputStream.class)))
            .of(typeLoader.load(InputStream.class))
            .build();

    Method method = getMethod(PagedInputStreamOperationStub.class, "paged", new Class[]{});
    when(operationModel.getModelProperty(ImplementingMethodModelProperty.class))
            .thenReturn(of(new ImplementingMethodModelProperty(method)));


    when(operationModel.getOutput().getType()).thenReturn(type);
    mockComponent(operationModel, builder(PagedInputStreamSampleDataProvider.class), PagedInputStreamSampleDataProvider.class.getSimpleName());

    validate();
    assertNoErrors();
  }

  private void assertWrongGenerics(ConnectableComponentModel model,
                                   Class<? extends SampleDataProvider> providerClass,
                                   String expectedError) {

    mockComponent(model, builder(providerClass), providerClass.getSimpleName());

    validate();
    assertErrorMessages(equalTo(expectedError));
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

  private void assertErrorMessages(Matcher<String>... matchers) {
    List<Problem> errors = problemsReporter.getErrors();
    assertThat(errors, hasSize(matchers.length));
    for (int i = 0; i < matchers.length; i++) {
      assertThat(errors.get(i).getMessage(), matchers[i]);
    }
  }

  private void validate() {
    validator.validate(extensionModel, problemsReporter);
  }

  private void mockComponent(ConnectableComponentModel componentModel,
                             SampleDataProviderFactoryModelPropertyBuilder builder,
                             String providerId) {
    when(componentModel.getModelProperty(SampleDataProviderFactoryModelProperty.class)).thenReturn(of(builder.build()));
    when(componentModel.getModelProperty(ImplementingParameterModelProperty.class)).thenReturn(empty());
    when(componentModel.getModelProperty(DeclaringMemberModelProperty.class)).thenReturn(empty());
    when(componentModel.getSampleDataProviderModel())
        .thenReturn(of(new SampleDataProviderModel(emptyList(), providerId, true, true)));
  }

  public static class PagedInputStreamOperationStub {

    public PagingProvider<Object, InputStream> paged() {
      return null;
    }
  }

  public static class TestSampleDataProvider implements SampleDataProvider<String, String> {

    @Connection
    protected String connection;

    @Override
    public Result<String, String> getSample() throws SampleDataException {
      return OUTPUT_RESULT;
    }

    @Override
    public String getId() {
      return getClass().getSimpleName();
    }
  }

  public static class ConnectedSampleDataProvider extends TestSampleDataProvider {
  }

  public static class ConfigAwareSampleDataProvider extends TestSampleDataProvider {

    @Config
    private Object config;
  }

  public static class NonInstantiableProvider extends TestSampleDataProvider {

    private NonInstantiableProvider() {}
  }

  public static class MapSampleDataProvider implements SampleDataProvider<Map<String, String>, String> {

    @Override
    public String getId() {
      return getClass().getSimpleName();
    }

    @Override
    public Result<Map<String, String>, String> getSample() throws SampleDataException {
      return null;
    }
  }

  public static class MapAttributesSampleDataProvider implements SampleDataProvider<String, Map<String, String>> {

    @Override
    public String getId() {
      return getClass().getSimpleName();
    }

    @Override
    public Result<String, Map<String, String>> getSample() throws SampleDataException {
      return null;
    }
  }

  public static class VoidAttributesSampleDataProvider implements SampleDataProvider<String, Void> {

    @Override
    public String getId() {
      return getClass().getSimpleName();
    }

    @Override
    public Result<String, Void> getSample() throws SampleDataException {
      return null;
    }
  }

  public static class PagedInputStreamSampleDataProvider implements SampleDataProvider<List<InputStream>, Void> {

    @Override
    public String getId() {
      return getClass().getSimpleName();
    }

    @Override
    public Result<List<InputStream>, Void> getSample() throws SampleDataException {
      return null;
    }
  }
}
