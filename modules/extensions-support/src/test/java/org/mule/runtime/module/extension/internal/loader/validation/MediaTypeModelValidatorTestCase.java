/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.stubbing.defaultanswers.ReturnsDeepStubs;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.impl.DefaultStringType;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ConnectableComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.model.ImmutableOutputModel;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.annotations.CustomDefinedStaticTypeAnnotation;
import org.mule.runtime.module.extension.internal.loader.java.property.CompileTimeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.SourceTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.springframework.core.ResolvableType;
import sun.reflect.annotation.ExceptionProxy;

import java.io.InputStream;
import java.lang.reflect.Method;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.validate;


@RunWith(MockitoJUnitRunner.class)
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

  @Before
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
    SourceTypeWrapper sourceType = new SourceTypeWrapper(sourceClass, typeLoader);
    ExtensionTypeDescriptorModelProperty extensionTypeDescriptorModelProperty =
        mock(ExtensionTypeDescriptorModelProperty.class, new ReturnsDeepStubs());
    when(sourceModel.getModelProperty(ExtensionTypeDescriptorModelProperty.class))
        .thenReturn(of(extensionTypeDescriptorModelProperty));
    when(extensionTypeDescriptorModelProperty.getType())
        .thenReturn(sourceType);
  }

  private void mockExtensionOperationDescriptorModelProperty(String operationMethodName) throws NoSuchMethodException {
    Method operationMethod = TestMethods.class.getMethod(operationMethodName);
    Type operationReturnType = new TypeWrapper(ResolvableType.forMethodReturnType(operationMethod), typeLoader);
    ExtensionOperationDescriptorModelProperty extensionOperationDescriptorModelProperty =
        mock(ExtensionOperationDescriptorModelProperty.class, new ReturnsDeepStubs());
    when(operationModel.getModelProperty(ExtensionOperationDescriptorModelProperty.class))
        .thenReturn(of(extensionOperationDescriptorModelProperty));
    when(extensionOperationDescriptorModelProperty.getOperationMethod().getReturnType()).thenReturn(operationReturnType);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void mediaTypeAnnotationMissingOnSourceWithStringOutput() throws Exception {
    mockExtensionTypeDescriptorModelProperty(TestStringSource.class);
    when(sourceOutputModel.getType()).thenReturn(toMetadataType(String.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void mediaTypeAnnotationMissingOnSourceWithStreamOutput() throws Exception {
    mockExtensionTypeDescriptorModelProperty(TestStreamSource.class);
    when(sourceOutputModel.getType()).thenReturn(toMetadataType(InputStream.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void mediaTypeAnnotationMissingOnOperationWithStringOutput() throws Exception {
    mockExtensionOperationDescriptorModelProperty("returnsString");
    when(operationOutputModel.getType()).thenReturn(toMetadataType(String.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void mediaTypeAnnotationMissingOnOperationWithStreamOutput() throws Exception {
    mockExtensionOperationDescriptorModelProperty("returnsStream");
    when(operationOutputModel.getType()).thenReturn(toMetadataType(InputStream.class));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void mediaTypeAnnotationAndStaticResolverOnOperation() throws Exception {
    mockExtensionOperationDescriptorModelProperty("returnsString");
    mockMediaTypeAnnotation(operationModel, "*/*", false);
    when(operationOutputModel.getType()).thenReturn(operationOutputType);
    when(operationOutputType.getAnnotation(CustomDefinedStaticTypeAnnotation.class))
        .thenReturn(of(new CustomDefinedStaticTypeAnnotation()));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void mediaTypeAnnotationAndStaticResolverOnSource() throws Exception {
    mockExtensionTypeDescriptorModelProperty(TestStringSource.class);
    mockMediaTypeAnnotation(sourceModel, "*/*", false);
    when(sourceOutputModel.getType()).thenReturn(sourceOutputType);
    when(sourceOutputType.getAnnotation(CustomDefinedStaticTypeAnnotation.class))
        .thenReturn(of(new CustomDefinedStaticTypeAnnotation()));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void mediaTypeAnnotationValueMissingOnOperation() throws Exception {
    mockExtensionOperationDescriptorModelProperty("returnsString");
    mockMediaTypeAnnotation(operationModel, EMPTY, false);
    when(operationOutputModel.getType()).thenReturn(operationOutputType);
    when(operationOutputType.getAnnotation(CustomDefinedStaticTypeAnnotation.class))
        .thenReturn(empty());
    when(operationOutputType.getMetadataFormat()).thenReturn(new MetadataFormat("java", "java", "application/java"));
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void mediaTypeAnnotationValueMissingOnSource() throws Exception {
    mockExtensionTypeDescriptorModelProperty(TestStringSource.class);
    mockMediaTypeAnnotation(sourceModel, EMPTY, false);
    when(sourceOutputModel.getType()).thenReturn(sourceOutputType);
    when(sourceOutputType.getAnnotation(CustomDefinedStaticTypeAnnotation.class))
        .thenReturn(empty());
    when(sourceOutputType.getMetadataFormat()).thenReturn(new MetadataFormat("java", "java", "application/java"));
    validate(extensionModel, validator);
  }

  @Test
  public void mediaTypeAnnotationWithDefaultValueAndStaticResolverOnOperation() throws Exception {
    mockExtensionOperationDescriptorModelProperty("returnsString");
    mockMediaTypeAnnotation(operationModel, EMPTY, false);
    when(operationOutputModel.getType()).thenReturn(operationOutputType);
    when(operationOutputType.getAnnotation(CustomDefinedStaticTypeAnnotation.class))
        .thenReturn(empty());
    when(operationOutputType.getMetadataFormat()).thenReturn(new MetadataFormat("Xml", "xml", "application/xml"));
    validate(extensionModel, validator);
  }

  @Test
  public void mediaTypeAnnotationWithDefaultValueAndStaticResolverOnSource() throws Exception {
    mockExtensionTypeDescriptorModelProperty(TestStringSource.class);
    mockMediaTypeAnnotation(sourceModel, EMPTY, false);
    when(sourceOutputModel.getType()).thenReturn(sourceOutputType);
    when(sourceOutputType.getAnnotation(CustomDefinedStaticTypeAnnotation.class))
        .thenReturn(empty());
    when(sourceOutputType.getMetadataFormat()).thenReturn(new MetadataFormat("Xml", "xml", "application/xml"));
    validate(extensionModel, validator);
  }


  @Test
  public void mediaTypeAnnotationAndStaticResolverOnOperationInRuntime() throws Exception {
    setCompileTime(false);
    mockExtensionOperationDescriptorModelProperty("returnsString");
    mockMediaTypeAnnotation(operationModel, "*/*", false);
    when(operationOutputModel.getType()).thenReturn(operationOutputType);
    when(operationOutputType.getAnnotation(CustomDefinedStaticTypeAnnotation.class))
        .thenReturn(of(new CustomDefinedStaticTypeAnnotation()));
    validate(extensionModel, validator);
  }

  @Test
  public void mediaTypeAnnotationAndStaticResolverOnSourceInRuntime() throws Exception {
    setCompileTime(false);
    mockExtensionTypeDescriptorModelProperty(TestStringSource.class);
    mockMediaTypeAnnotation(sourceModel, "*/*", false);
    when(sourceOutputModel.getType()).thenReturn(sourceOutputType);
    when(sourceOutputType.getAnnotation(CustomDefinedStaticTypeAnnotation.class))
        .thenReturn(of(new CustomDefinedStaticTypeAnnotation()));
    validate(extensionModel, validator);
  }


  @Test
  public void sourceAndOperationReturnsAnObject() throws Exception {
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

  /**
   * SUCESS SCENARIOS
   *
   * has static resolver, no media type annotation metadata format is XML, no static resolver media type annotation configured, no
   * static resolver annotation with value + static resolver but in runtime
   *
   */


}
