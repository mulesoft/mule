/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.validate;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.visitableMock;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ConnectableComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.api.loader.java.type.SourceElement;
import org.mule.runtime.module.extension.internal.loader.annotations.CustomDefinedStaticTypeAnnotation;
import org.mule.runtime.module.extension.internal.loader.java.property.CompileTimeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.OperationWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.SourceTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.java.validation.MediaTypeModelValidator;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.InputStream;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class MediaTypeModelValidatorTestCase extends AbstractMuleTestCase {

  private static final String EMPTY = "";

  private ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionModel extensionModel;

  @Mock
  private OperationModel operationModel;

  @Mock
  private SourceModel sourceModel;

  @Mock
  private ExtensionTypeDescriptorModelProperty descriptorModelProperty;

  @Mock
  private ExtensionOperationDescriptorModelProperty operationDescriptorModelProperty;

  @Mock
  private OutputModel sourceOutputModel;

  @Mock
  private OutputModel operationOutputModel;

  @Mock
  private MetadataType operationOutputType;

  @Mock
  private MetadataType sourceOutputType;

  private MediaTypeModelValidator validator = new MediaTypeModelValidator();

  @BeforeEach
  public void before() throws Exception {
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(extensionModel.getSourceModels()).thenReturn(asList(sourceModel));

    when(sourceModel.getSuccessCallback()).thenReturn(empty());
    when(sourceModel.getErrorCallback()).thenReturn(empty());
    when(sourceModel.getModelProperty(ExtensionTypeDescriptorModelProperty.class)).thenReturn(of(descriptorModelProperty));
    when(operationModel.getModelProperty(ExtensionOperationDescriptorModelProperty.class))
        .thenReturn(of(operationDescriptorModelProperty));

    when(sourceModel.getModelProperty(MediaTypeModelProperty.class)).thenReturn(empty());
    when(operationModel.getModelProperty(MediaTypeModelProperty.class)).thenReturn(empty());

    when(operationModel.getOutput()).thenReturn(operationOutputModel);
    when(sourceModel.getOutput()).thenReturn(sourceOutputModel);

    when(operationOutputModel.getType()).thenReturn(toMetadataType(Object.class));
    when(sourceOutputModel.getType()).thenReturn(toMetadataType(Object.class));

    when(sourceModel.getName()).thenReturn("source");
    when(operationModel.getName()).thenReturn("operation");

    mockExtensionOperationDescriptorModelProperty("returnsObject");
    mockExtensionTypeDescriptorModelProperty(TestObjectSource.class);

    setCompileTime(true);
    visitableMock(operationModel, sourceModel);
  }

  private void setCompileTime(boolean compileTime) {
    when(extensionModel.getModelProperty(CompileTimeModelProperty.class))
        .thenReturn(ofNullable(compileTime ? new CompileTimeModelProperty() : null));
  }

  private void mockMediaTypeAnnotation(ConnectableComponentModel model, String value, boolean strict) {
    when(model.getModelProperty(MediaTypeModelProperty.class)).thenReturn(ofNullable(new MediaTypeModelProperty(value, strict)));
  }

  private void mockExtensionTypeDescriptorModelProperty(Class sourceClass) {
    SourceElement sourceElement = new SourceTypeWrapper(sourceClass, typeLoader);
    when(sourceModel.getModelProperty(ExtensionTypeDescriptorModelProperty.class))
        .thenReturn(of(new ExtensionTypeDescriptorModelProperty(sourceElement)));
  }

  private void mockExtensionOperationDescriptorModelProperty(String operationMethodName) throws NoSuchMethodException {
    Method operationMethod = TestMethods.class.getMethod(operationMethodName);
    OperationElement operationElement = new OperationWrapper(operationMethod, typeLoader);
    when(operationModel.getModelProperty(ExtensionOperationDescriptorModelProperty.class))
        .thenReturn(of(new ExtensionOperationDescriptorModelProperty(operationElement)));
  }

  @Test
  void mediaTypeAnnotationMissingOnSourceWithStringOutput() throws Exception {
    mockExtensionTypeDescriptorModelProperty(TestStringSource.class);
    when(sourceOutputModel.getType()).thenReturn(toMetadataType(String.class));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(), containsString("'source' has a String type output but doesn't specify a default mime type."));
  }

  @Test
  void mediaTypeAnnotationMissingOnSourceWithStreamOutput() throws Exception {
    mockExtensionTypeDescriptorModelProperty(TestStreamSource.class);
    when(sourceOutputModel.getType()).thenReturn(toMetadataType(InputStream.class));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("Source 'source' has a InputStream type output but doesn't specify a default mime type."));
  }

  @Test
  void mediaTypeAnnotationMissingOnOperationWithStringOutput() throws Exception {
    mockExtensionOperationDescriptorModelProperty("returnsString");
    when(operationOutputModel.getType()).thenReturn(toMetadataType(String.class));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("Operation 'operation' has a String type output but doesn't specify a default mime type."));
  }

  @Test
  void mediaTypeAnnotationMissingOnOperationWithStreamOutput() throws Exception {
    mockExtensionOperationDescriptorModelProperty("returnsStream");
    when(operationOutputModel.getType()).thenReturn(toMetadataType(InputStream.class));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("Operation 'operation' has a InputStream type output but doesn't specify a default mime type."));
  }

  @Test
  void mediaTypeAnnotationAndStaticResolverOnOperation() throws Exception {
    mockExtensionOperationDescriptorModelProperty("returnsString");
    mockMediaTypeAnnotation(operationModel, "*/*", true);
    when(operationOutputModel.getType()).thenReturn(operationOutputType);
    when(operationOutputType.getAnnotation(CustomDefinedStaticTypeAnnotation.class))
        .thenReturn(of(new CustomDefinedStaticTypeAnnotation()));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("Operation 'operation' is declaring both a custom output Type using a Static MetadataResolver, and a custom media type through the @MediaType annotation."));
  }

  @Test
  void mediaTypeAnnotationAndStaticResolverOnSource() throws Exception {
    mockExtensionTypeDescriptorModelProperty(TestStringSource.class);
    mockMediaTypeAnnotation(sourceModel, "*/*", true);
    when(sourceOutputModel.getType()).thenReturn(sourceOutputType);
    when(sourceOutputType.getAnnotation(CustomDefinedStaticTypeAnnotation.class))
        .thenReturn(of(new CustomDefinedStaticTypeAnnotation()));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("Source 'source' is declaring both a custom output Type using a Static MetadataResolver, and a custom media type through the @MediaType annotation."));
  }

  @Test
  void mediaTypeAnnotationValueMissingOnOperation() throws Exception {
    mockExtensionOperationDescriptorModelProperty("returnsString");
    mockMediaTypeAnnotation(operationModel, EMPTY, false);
    when(operationOutputModel.getType()).thenReturn(operationOutputType);
    when(operationOutputType.getAnnotation(CustomDefinedStaticTypeAnnotation.class))
        .thenReturn(empty());
    when(operationOutputType.getMetadataFormat()).thenReturn(new MetadataFormat("java", "java", "application/java"));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("Operation 'operation' has a String type output but doesn't specify a default mime type."));
  }

  @Test
  void mediaTypeAnnotationValueMissingOnSource() throws Exception {
    mockExtensionTypeDescriptorModelProperty(TestStringSource.class);
    mockMediaTypeAnnotation(sourceModel, EMPTY, false);
    when(sourceOutputModel.getType()).thenReturn(sourceOutputType);
    when(sourceOutputType.getAnnotation(CustomDefinedStaticTypeAnnotation.class))
        .thenReturn(empty());
    when(sourceOutputType.getMetadataFormat()).thenReturn(new MetadataFormat("java", "java", "application/java"));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("Source 'source' has a String type output but doesn't specify a default mime type."));
  }

  @Test
  void mediaTypeAnnotationWithDefaultValueAndStaticResolverOnOperation() throws Exception {
    mockExtensionOperationDescriptorModelProperty("returnsString");
    mockMediaTypeAnnotation(operationModel, EMPTY, false);
    mockMediaTypeAnnotation(sourceModel, "*/*", false);
    when(operationOutputModel.getType()).thenReturn(operationOutputType);
    when(operationOutputType.getAnnotation(CustomDefinedStaticTypeAnnotation.class))
        .thenReturn(empty());
    when(operationOutputType.getMetadataFormat()).thenReturn(new MetadataFormat("Xml", "xml", "application/xml"));
    validate(extensionModel, validator);
  }

  @Test
  void mediaTypeAnnotationWithDefaultValueAndStaticResolverOnSource() throws Exception {
    mockExtensionTypeDescriptorModelProperty(TestStringSource.class);
    mockMediaTypeAnnotation(sourceModel, EMPTY, false);
    mockMediaTypeAnnotation(operationModel, "*/*", false);
    when(sourceOutputModel.getType()).thenReturn(sourceOutputType);
    when(sourceOutputType.getAnnotation(CustomDefinedStaticTypeAnnotation.class))
        .thenReturn(empty());
    when(sourceOutputType.getMetadataFormat()).thenReturn(new MetadataFormat("Xml", "xml", "application/xml"));
    validate(extensionModel, validator);
  }

  @Test
  void mediaTypeAnnotationAndStaticResolverOnOperationInRuntime() throws Exception {
    setCompileTime(false);
    mockExtensionOperationDescriptorModelProperty("returnsString");
    mockMediaTypeAnnotation(operationModel, "*/*", false);
    when(operationOutputModel.getType()).thenReturn(operationOutputType);
    when(operationOutputType.getAnnotation(CustomDefinedStaticTypeAnnotation.class))
        .thenReturn(of(new CustomDefinedStaticTypeAnnotation()));
    validate(extensionModel, validator);
  }

  @Test
  void mediaTypeAnnotationAndStaticResolverOnSourceInRuntime() throws Exception {
    setCompileTime(false);
    mockExtensionTypeDescriptorModelProperty(TestStringSource.class);
    mockMediaTypeAnnotation(sourceModel, "*/*", false);
    when(sourceOutputModel.getType()).thenReturn(sourceOutputType);
    when(sourceOutputType.getAnnotation(CustomDefinedStaticTypeAnnotation.class))
        .thenReturn(of(new CustomDefinedStaticTypeAnnotation()));
    validate(extensionModel, validator);
  }

  @Test
  void sourceAndOperationReturnsAnObject() throws Exception {
    mockMediaTypeAnnotation(operationModel, "*/*", false);
    mockMediaTypeAnnotation(sourceModel, "*/*", false);
    validate(extensionModel, validator);
  }

  public class TestMethods {

    public Object returnsObject() {
      return null;
    }

    public String returnsString() {
      return null;
    }

    public InputStream returnsStream() {
      return null;
    }

  }

  public class TestObjectSource extends Source<Object, Object> {

    @Override
    public void onStart(SourceCallback<Object, Object> sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }

  public class TestStringSource extends Source<String, Object> {

    @Override
    public void onStart(SourceCallback<String, Object> sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }

  public class TestStreamSource extends Source<InputStream, Object> {

    @Override
    public void onStart(SourceCallback<InputStream, Object> sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }

}
