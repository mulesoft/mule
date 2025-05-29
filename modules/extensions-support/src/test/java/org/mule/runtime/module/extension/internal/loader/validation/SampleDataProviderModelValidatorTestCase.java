/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.core.api.util.ClassUtils.getMethod;
import static org.mule.runtime.module.extension.internal.loader.java.property.SampleDataProviderFactoryModelProperty.builder;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.visitableMock;

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
import static org.mockito.quality.Strictness.LENIENT;

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
import org.mule.runtime.module.extension.internal.loader.java.validation.JavaSampleDataModelValidator;
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
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@SmallTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class SampleDataProviderModelValidatorTestCase {

  private static Result<String, String> OUTPUT_RESULT;
  private final JavaTypeLoader loader = new JavaTypeLoader(this.getClass().getClassLoader());
  private final MetadataType STRING_TYPE = loader.load(String.class);
  private final MetadataType NUMBER_TYPE = loader.load(Integer.class);
  private final ReflectionCache reflectionCache = new ReflectionCache();

  private JavaSampleDataModelValidator validator;
  private ProblemsReporter problemsReporter;

  @Mock
  private ExtensionModel extensionModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private OperationModel operationModel;

  @Mock
  private ParameterModel parameterModel;

  @Mock
  private ConfigurationModel configurationModel;

  @Mock
  private ParameterGroupModel parameterGroupModel;

  @Mock
  private ParameterGroupModel configurationParameterGroupModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private SourceModel sourceModel;

  private SampleDataProviderFactoryModelPropertyBuilder providerBuilder;

  private ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @BeforeEach
  public void setUp() {
    OUTPUT_RESULT = mock(Result.class);
    validator = new JavaSampleDataModelValidator();
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

  @AfterEach
  public void tearDown() {
    OUTPUT_RESULT = null;
  }

  @Test
  void providerShouldBeInstantiable() {
    SampleDataProviderFactoryModelPropertyBuilder builder = builder(NonInstantiableProvider.class);
    mockComponent(operationModel, builder, "anotherId");

    validate();
    assertProblems("The SampleDataProvider [NonInstantiableProvider] is not instantiable");
  }

  @Test
  void parameterShouldExist() {
    providerBuilder.withInjectableParameter("someParam", STRING_TYPE, true);
    when(operationModel.getModelProperty(SampleDataProviderFactoryModelProperty.class))
        .thenReturn(of(providerBuilder.build()));

    mockOperationProvider();

    validate();
    assertProblems("The SampleDataProvider [ConnectedSampleDataProvider] declares to use a parameter 'someParam' which doesn't exist in the operation 'superOperation'");
  }

  @Test
  void parameterShouldBeOfSameType() {
    providerBuilder.withInjectableParameter("someName", NUMBER_TYPE, true);
    mockOperationProvider();

    validate();
    assertProblems("The SampleDataProvider [ConnectedSampleDataProvider] defines a parameter 'someName' of type 'class java.lang.Integer' but in the operation 'superOperation' is of type 'class java.lang.String'");
  }

  @Test
  void injectConnectionInConnectionLessComponent() {
    providerBuilder.withConnection(getField(ConnectedSampleDataProvider.class, "connection", reflectionCache).get());
    when(operationModel.requiresConnection()).thenReturn(false);
    mockOperationProvider();

    validate();
    assertProblems("The SampleDataProvider [ConnectedSampleDataProvider] defines that it requires a connection, but is used in the operation 'superOperation' which is connection less");
  }

  @Test
  void injectConfigInConfigLessComponent() {
    providerBuilder.withConfig(getField(ConfigAwareSampleDataProvider.class, "config", reflectionCache).get());
    mockOperationProvider();

    validate();
    assertProblems("The SampleDataProvider [ConnectedSampleDataProvider] defines that it requires a config, but is used in the operation 'superOperation' which is config less");
  }

  @Test
  void injectConfigInConfigAwareComponent() {
    providerBuilder.withConfig(getField(ConfigAwareSampleDataProvider.class, "config", reflectionCache).get());
    when(configurationModel.getOperationModels()).thenReturn(asList(operationModel));
    when(extensionModel.getOperationModels()).thenReturn(emptyList());
    mockOperationProvider();

    validate();
  }

  @Test
  void duplicateProviderId() {
    mockOperationProvider();
    String id = ConnectedSampleDataProvider.class.getSimpleName();
    mockComponent(sourceModel, builder(ConfigAwareSampleDataProvider.class), id);

    validate();
    assertProblems(format("The following SampleDataProvider implementations [%s, %s] use the same id [%s]. SampleDataProvider ids must be unique.",
                          ConfigAwareSampleDataProvider.class.getName(), ConnectedSampleDataProvider.class.getName(), id));
  }

  @Test
  void legalComponent() {
    mockOperationProvider();
    mockSourceProvider();

    validate();
    assertNoErrors();
  }

  @Test
  void operationWithWrongPayloadTypeSampleDataProvider() {
    assertWrongGenerics(operationModel,
                        MapSampleDataProvider.class,
                        "SampleDataProvider [org.mule.runtime.module.extension.internal.loader.validation.SampleDataProviderModelValidatorTestCase$MapSampleDataProvider] is used at component 'superOperation' which outputs a Result<java.lang.String, java.lang.String>, but the provider generic signature is '<java.util.Map<java.lang.String, java.lang.String>, java.lang.String>'");
  }

  @Test
  void operationWithWrongAttributesTypeSampleDataProvider() {
    assertWrongGenerics(operationModel,
                        MapAttributesSampleDataProvider.class,
                        "SampleDataProvider [org.mule.runtime.module.extension.internal.loader.validation.SampleDataProviderModelValidatorTestCase$MapAttributesSampleDataProvider] is used at component 'superOperation' which outputs a Result<java.lang.String, java.lang.String>, but the provider generic signature is '<java.lang.String, java.util.Map<java.lang.String, java.lang.String>>'");
  }

  @Test
  void sourceWithWrongPayloadTypeSampleDataProvider() {
    assertWrongGenerics(sourceModel,
                        MapSampleDataProvider.class,
                        "SampleDataProvider [org.mule.runtime.module.extension.internal.loader.validation.SampleDataProviderModelValidatorTestCase$MapSampleDataProvider] is used at component 'listener' which outputs a Result<java.lang.String, java.lang.String>, but the provider generic signature is '<java.util.Map<java.lang.String, java.lang.String>, java.lang.String>'");
  }

  @Test
  void sourceWithWrongAttributesTypeSampleDataProvider() {
    assertWrongGenerics(sourceModel,
                        MapAttributesSampleDataProvider.class,
                        "SampleDataProvider [org.mule.runtime.module.extension.internal.loader.validation.SampleDataProviderModelValidatorTestCase$MapAttributesSampleDataProvider] is used at component 'listener' which outputs a Result<java.lang.String, java.lang.String>, but the provider generic signature is '<java.lang.String, java.util.Map<java.lang.String, java.lang.String>>'");
  }

  @Test
  void operationWithBoxedVoidAttributes() {
    mockComponent(operationModel, builder(VoidAttributesSampleDataProvider.class),
                  VoidAttributesSampleDataProvider.class.getSimpleName());

    when(operationModel.getOutputAttributes().getType()).thenReturn(typeLoader.load(Void.class));

    validate();
    assertNoErrors();
  }

  @Test
  void operationWithNativeVoidAttributes() {
    mockComponent(operationModel, builder(VoidAttributesSampleDataProvider.class),
                  VoidAttributesSampleDataProvider.class.getSimpleName());

    when(operationModel.getOutputAttributes().getType()).thenReturn(typeLoader.load(void.class));

    validate();
    assertNoErrors();
  }

  @Test
  void operationWithBoxedVoidReturnType() {
    mockComponent(operationModel, builder(VoidReturnTypeSampleDataProvider.class),
                  VoidAttributesSampleDataProvider.class.getSimpleName());

    validate();
    assertErrorMessages(equalTo("SampleDataProvider [org.mule.runtime.module.extension.internal.loader.validation.SampleDataProviderModelValidatorTestCase$VoidReturnTypeSampleDataProvider] cannot have a Void return type"));
  }

  @Test
  void pagingOperationWithInputStreamPayload() {
    Method method = getPagedOperationMethod();
    mockInputStreamPaging(method);

    mockComponent(operationModel, builder(PagedInputStreamSampleDataProvider.class),
                  PagedInputStreamSampleDataProvider.class.getSimpleName());

    validate();
    assertNoErrors();
  }

  @Test
  void pagingOperationWithSampleProviderWhichDoesNotReturnCollection() {
    Method method = getPagedOperationMethod();
    mockInputStreamPaging(method);

    mockComponent(operationModel, builder(TestSampleDataProvider.class), TestSampleDataProvider.class.getSimpleName());

    validate();
    assertErrorMessages(equalTo("SampleDataProvider [org.mule.runtime.module.extension.internal.loader.validation.SampleDataProviderModelValidatorTestCase$TestSampleDataProvider] is used on component 'superOperation' which is paged. The SampleDataProvider is thus expected to provide a payload of type 'Collection<java.io.InputStream>' but it returns a payload of type 'java.lang.String' instead"));
  }

  @Test
  void pagingOperationWithSampleProviderWhichReturnsCollectionOfWrongType() {
    Method method = getPagedOperationMethod();
    mockInputStreamPaging(method);
    mockComponent(operationModel, builder(PagedStringSampleDataProvider.class),
                  PagedStringSampleDataProvider.class.getSimpleName());

    validate();
    assertErrorMessages(equalTo("SampleDataProvider [org.mule.runtime.module.extension.internal.loader.validation.SampleDataProviderModelValidatorTestCase$PagedStringSampleDataProvider] is used on component 'superOperation' which is paged. The SampleDataProvider is thus expected to provide a payload of type 'Collection<java.io.InputStream>', but a Collection<java.lang.String> was found instead."));
  }

  @Test
  void pagingOperationWithUnboundedSampleProvider() {
    Method method = getPagedOperationMethod();
    mockInputStreamPaging(method);

    mockComponent(operationModel, builder(UnboundedPagingSampleDataProvider.class),
                  UnboundedPagingSampleDataProvider.class.getSimpleName());

    validate();
    assertErrorMessages(equalTo("SampleDataProvider [org.mule.runtime.module.extension.internal.loader.validation.SampleDataProviderModelValidatorTestCase$UnboundedPagingSampleDataProvider] is used on component 'superOperation' which is paged. The SampleDataProvider is thus expected to provide a payload of type 'Collection<java.io.InputStream>', but an unbounded Collection was found instead. Please provide the proper generic"));
  }

  @Test
  void pagingOperationWithSampleProviderWhichReturnsAttributesOfWrongType() {
    Method method = getPagedOperationMethod();
    mockInputStreamPaging(method);

    mockComponent(operationModel, builder(PagingSampleDataProviderWithWrongAttributesType.class),
                  PagingSampleDataProviderWithWrongAttributesType.class.getSimpleName());

    validate();
    assertErrorMessages(equalTo("SampleDataProvider [org.mule.runtime.module.extension.internal.loader.validation.SampleDataProviderModelValidatorTestCase$PagingSampleDataProviderWithWrongAttributesType] is used on component 'superOperation' which is paged. The SampleDataProvider is thus expected to provide attributes of type 'java.lang.String' but it returns attributes of type 'void' instead"));
  }

  @Test
  void operationSamePayloadTypeUsingGenericsSampleDataProvider() {
    mockComponent(operationModel, builder(SampleDataProviderWithGenerics.class),
                  SampleDataProviderWithGenerics.class.getSimpleName());
    ParameterizedType outputType = TypeUtils.parameterize(Map.class, String.class, Object.class);
    when(operationModel.getOutput().getType()).thenReturn(typeLoader.load(outputType));
    when(operationModel.getOutputAttributes().getType()).thenReturn(typeLoader.load(String.class));

    validate();
    assertNoErrors();
  }

  @Test
  void operationSamePayloadTypeUsingNestedGenericsSampleDataProvider() {
    mockComponent(operationModel, builder(SampleDataProviderWithNestedGenerics.class),
                  SampleDataProviderWithNestedGenerics.class.getSimpleName());
    ParameterizedType listType = TypeUtils.parameterize(List.class, String.class);
    ParameterizedType outputType = TypeUtils.parameterize(Map.class, String.class, listType);
    when(operationModel.getOutput().getType()).thenReturn(typeLoader.load(outputType));
    when(operationModel.getOutputAttributes().getType()).thenReturn(typeLoader.load(String.class));

    validate();
    assertNoErrors();
  }

  @Test
  void operationSamePayloadTypeUsingWrongGenericsSampleDataProvider() {
    mockComponent(operationModel, builder(SampleDataProviderWithGenerics.class),
                  SampleDataProviderWithGenerics.class.getSimpleName());
    ParameterizedType outputType = TypeUtils.parameterize(Map.class, Integer.class, Object.class);
    when(operationModel.getOutput().getType()).thenReturn(typeLoader.load(outputType));
    when(operationModel.getOutputAttributes().getType()).thenReturn(typeLoader.load(String.class));

    validate();
    assertErrorMessages(equalTo("SampleDataProvider [org.mule.runtime.module.extension.internal.loader.validation.SampleDataProviderModelValidatorTestCase$SampleDataProviderWithGenerics] is used at component 'superOperation' which outputs a Result<java.util.Map<java.lang.Integer, java.lang.Object>, java.lang.String>, but the provider generic signature is '<java.util.Map<java.lang.String, java.lang.Object>, java.lang.String>'"));
  }

  @Test
  void operationSamePayloadTypeUsingWrongNestedGenericsSampleDataProvider() {
    mockComponent(operationModel, builder(SampleDataProviderWithNestedGenerics.class),
                  SampleDataProviderWithNestedGenerics.class.getSimpleName());
    ParameterizedType listType = TypeUtils.parameterize(List.class, Object.class);
    ParameterizedType outputType = TypeUtils.parameterize(Map.class, String.class, listType);
    when(operationModel.getOutput().getType()).thenReturn(typeLoader.load(outputType));
    when(operationModel.getOutputAttributes().getType()).thenReturn(typeLoader.load(String.class));

    validate();
    assertNoErrors();
  }

  @Test
  void boundParameterExists() {
    providerBuilder.withInjectableParameter("actingParameter", STRING_TYPE, true, "someName");
    when(operationModel.getModelProperty(SampleDataProviderFactoryModelProperty.class))
        .thenReturn(of(providerBuilder.build()));

    mockOperationProvider();

    validate();
    assertNoErrors();
  }

  @Test
  void boundParameterShouldExist() {
    providerBuilder.withInjectableParameter("actingParameter", STRING_TYPE, true, "anotherName");
    when(operationModel.getModelProperty(SampleDataProviderFactoryModelProperty.class))
        .thenReturn(of(providerBuilder.build()));

    mockOperationProvider();

    validate();
    assertProblems("The SampleDataProvider [ConnectedSampleDataProvider] declares to use a parameter 'anotherName' which doesn't exist in the operation 'superOperation'");
  }

  @Test
  void boundParameterFromExtractionExpressionExists() {
    providerBuilder.withInjectableParameter("actingParameter", STRING_TYPE, true, "someName.someTag.@attribute");
    when(operationModel.getModelProperty(SampleDataProviderFactoryModelProperty.class))
        .thenReturn(of(providerBuilder.build()));

    mockOperationProvider();

    validate();
    assertNoErrors();
  }

  @Test
  void boundParameterFromExtractionExpressionShouldExist() {
    providerBuilder.withInjectableParameter("actingParameter", STRING_TYPE, true, "anotherName.nested.fields");
    when(operationModel.getModelProperty(SampleDataProviderFactoryModelProperty.class))
        .thenReturn(of(providerBuilder.build()));

    mockOperationProvider();

    validate();
    assertProblems("The SampleDataProvider [ConnectedSampleDataProvider] declares to use a parameter 'anotherName' which doesn't exist in the operation 'superOperation'");
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

  /**
   * The {@code method} is received as an argument instead of just being fetched internally here because the
   * {@link Method#getGenericReturnType()} method internally relies on a {@link java.lang.ref.WeakReference}. If the
   * {@code method} is not held in a variable for the duration of the test, it becomes flaky and randomly fails with NPE.
   */
  private void mockInputStreamPaging(Method method) {
    MetadataType type = create(JAVA).arrayType()
        .with(new ClassInformationAnnotation(PagingProvider.class, asList(Object.class, InputStream.class)))
        .of(typeLoader.load(InputStream.class))
        .build();

    when(operationModel.getModelProperty(ImplementingMethodModelProperty.class))
        .thenReturn(of(new ImplementingMethodModelProperty(method)));
    when(operationModel.getOutput().getType()).thenReturn(type);
  }

  private Method getPagedOperationMethod() {
    return getMethod(PagedInputStreamOperationStub.class, "paged", new Class[] {});
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

  public static class VoidReturnTypeSampleDataProvider implements SampleDataProvider<Void, Void> {

    @Override
    public String getId() {
      return getClass().getSimpleName();
    }

    @Override
    public Result<Void, Void> getSample() throws SampleDataException {
      return null;
    }
  }

  public static class PagedInputStreamSampleDataProvider implements SampleDataProvider<List<InputStream>, String> {

    @Override
    public String getId() {
      return getClass().getSimpleName();
    }

    @Override
    public Result<List<InputStream>, String> getSample() throws SampleDataException {
      return null;
    }
  }

  public static class PagedStringSampleDataProvider implements SampleDataProvider<List<String>, String> {

    @Override
    public String getId() {
      return getClass().getSimpleName();
    }

    @Override
    public Result<List<String>, String> getSample() throws SampleDataException {
      return null;
    }
  }

  public static class UnboundedPagingSampleDataProvider implements SampleDataProvider<List, String> {

    @Override
    public String getId() {
      return getClass().getSimpleName();
    }

    @Override
    public Result<List, String> getSample() throws SampleDataException {
      return null;
    }
  }

  public static class PagingSampleDataProviderWithWrongAttributesType implements SampleDataProvider<List<InputStream>, Void> {

    @Override
    public String getId() {
      return getClass().getSimpleName();
    }

    @Override
    public Result<List<InputStream>, Void> getSample() throws SampleDataException {
      return null;
    }
  }

  public static class SampleDataProviderWithGenerics implements SampleDataProvider<Map<String, Object>, String> {

    @Override
    public String getId() {
      return getClass().getSimpleName();
    }

    @Override
    public Result<Map<String, Object>, String> getSample() throws SampleDataException {
      return null;
    }
  }

  public static class SampleDataProviderWithNestedGenerics implements SampleDataProvider<Map<String, List<String>>, String> {

    @Override
    public String getId() {
      return getClass().getSimpleName();
    }

    @Override
    public Result<Map<String, List<String>>, String> getSample() throws SampleDataException {
      return null;
    }
  }

}
