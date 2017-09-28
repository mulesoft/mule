/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OutputDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOutputDeclaration;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class MimeTypeParametersDeclarationEnricherTestCase extends AbstractMuleTestCase {

  private static final BaseTypeBuilder builder = BaseTypeBuilder.create(JAVA);

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionLoadingContext extensionLoadingContext;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionDeclarer extensionDeclarer;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionDeclaration extensionDeclaration;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private OperationDeclaration operation;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private SourceDeclaration source;

  private MimeTypeParametersDeclarationEnricher enricher = new MimeTypeParametersDeclarationEnricher();

  @Before
  public void before() {
    when(extensionLoadingContext.getExtensionDeclarer()).thenReturn(extensionDeclarer);
    when(extensionDeclarer.getDeclaration()).thenReturn(extensionDeclaration);
    when(extensionDeclaration.getOperations()).thenReturn(singletonList(operation));
    when(extensionDeclaration.getMessageSources()).thenReturn(singletonList(source));

    when(source.getSuccessCallback()).thenReturn(empty());
    when(source.getErrorCallback()).thenReturn(empty());
    when(operation.getParameterGroup(DEFAULT_GROUP_NAME)).thenReturn(new ParameterGroupDeclaration(DEFAULT_GROUP_NAME));

    when(source.getParameterGroup(DEFAULT_GROUP_NAME)).thenReturn(new ParameterGroupDeclaration(DEFAULT_GROUP_NAME));

    mockMediaType(operation, false);
    mockMediaType(source, false);
  }

  private void mockMediaType(BaseDeclaration declaration, boolean strict) {
    when(declaration.getModelProperty(MediaTypeModelProperty.class)).thenReturn(Optional.of(
                                                                                            new MediaTypeModelProperty(TEXT_PLAIN,
                                                                                                                       strict)));
  }

  @Test
  public void binaryTypeOperation() {
    mockOutput(operation, builder.binaryType().build());
    enricher.enrich(extensionLoadingContext);
    assertMimeTypeParams(operation);
  }

  @Test
  public void binaryTypeOperationWithStrictMimeType() {
    mockOutput(operation, builder.binaryType().build());
    mockMediaType(operation, true);
    enricher.enrich(extensionLoadingContext);
    assertNoMimeTypeParams(operation);
  }

  @Test
  public void objectTypeOperation() {
    mockOutput(operation, builder.objectType().build());
    enricher.enrich(extensionLoadingContext);
    assertThat(getGroupParameters(operation), hasSize(0));
  }

  @Test
  public void enumTypeOperation() {
    StringType type = builder.stringType().with(new EnumAnnotation<>(new String[] {"val"})).build();
    mockOutput(operation, type);
    enricher.enrich(extensionLoadingContext);
    assertThat(getGroupParameters(operation), hasSize(0));
  }

  @Test
  public void stringOperation() {
    mockOutput(operation, builder.stringType().build());
    enricher.enrich(extensionLoadingContext);
    List<ParameterDeclaration> params = getGroupParameters(operation);
    assertThat(params, hasSize(1));
    assertParameter(params.get(0), MIME_TYPE_PARAMETER_NAME);
  }

  @Test
  public void stringOperationWithStrictMimeType() {
    mockOutput(operation, builder.stringType().build());
    mockMediaType(operation, true);
    enricher.enrich(extensionLoadingContext);
    assertNoMimeTypeParams(operation);
  }

  @Test
  public void stringTypeSource() {
    mockOutput(source, builder.stringType().build());
    enricher.enrich(extensionLoadingContext);
    List<ParameterDeclaration> params = getGroupParameters(source);
    assertThat(params, hasSize(1));
    assertParameter(params.get(0), MIME_TYPE_PARAMETER_NAME);
  }

  @Test
  public void stringTypeSourceWithStrictMimeType() {
    mockOutput(source, builder.stringType().build());
    mockMediaType(source, true);
    enricher.enrich(extensionLoadingContext);
    assertNoMimeTypeParams(source);
  }

  @Test
  public void inputStreamTypeSource() {
    mockOutput(source, builder.binaryType().build());
    enricher.enrich(extensionLoadingContext);
    assertMimeTypeParams(source);
  }

  @Test
  public void inputStreamTypeSourceWithStrictMimeType() {
    mockOutput(source, builder.binaryType().build());
    mockMediaType(source, true);
    enricher.enrich(extensionLoadingContext);
    assertNoMimeTypeParams(source);
  }

  @Test
  public void objectTypeSource() {
    mockOutput(source, builder.objectType().build());
    enricher.enrich(extensionLoadingContext);
    assertThat(getGroupParameters(source), hasSize(0));
  }

  private void assertNoMimeTypeParams(ParameterizedDeclaration<?> withParams) {
    List<ParameterDeclaration> parameters = withParams.getParameterGroup(DEFAULT_GROUP_NAME).getParameters();
    assertThat(parameters, hasSize(0));
  }

  private void assertMimeTypeParams(ParameterizedDeclaration<?> withParams) {
    List<ParameterDeclaration> parameters = withParams.getParameterGroup(DEFAULT_GROUP_NAME).getParameters();
    assertThat(parameters, hasSize(2));
    assertParameter(parameters.get(0), MIME_TYPE_PARAMETER_NAME);
    assertParameter(parameters.get(1), ENCODING_PARAMETER_NAME);
  }

  private void assertParameter(ParameterDeclaration parameter, String name) {
    assertThat(parameter, is(notNullValue()));
    assertThat(parameter.getName(), is(name));
    assertThat(parameter.getType(), equalTo(toMetadataType(String.class)));
    assertThat(parameter.isRequired(), is(false));
    assertThat(parameter.getExpressionSupport(), is(SUPPORTED));
    assertThat(parameter.getDefaultValue(), is(nullValue()));
  }

  private void mockOutput(WithOutputDeclaration declaration, MetadataType type) {
    OutputDeclaration output = mock(OutputDeclaration.class);
    when(output.getType()).thenReturn(type);
    when(declaration.getOutput()).thenReturn(output);
  }

  public class TestMethods {

    public void voidMethod() {}

    public Result<String, Object> resultString() {
      return null;
    }

    public Result<Apple, Object> resultApple() {
      return null;
    }

    public Object objectMethod() {
      return null;
    }

    public ByteArrayInputStream inputStreamMethod() {
      return null;
    }

    public Apple appleMethod() {
      return null;
    }

    public TestEnum enumMethod() {
      return TestEnum.ENUM1;
    }
  }

  public enum TestEnum {
    ENUM1, ENUM2
  }

  public class TestStringSource extends Source<String, Object> {

    @Override
    public void onStart(SourceCallback<String, Object> sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }

  public class TestAppleSource extends Source<Apple, Object> {

    @Override
    public void onStart(SourceCallback<Apple, Object> sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }

  public class TestNoGenericsSource extends Source {

    @Override
    public void onStart(SourceCallback sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }

  private List<ParameterDeclaration> getGroupParameters(ComponentDeclaration declaration) {
    return declaration.getParameterGroup(DEFAULT_GROUP_NAME).getParameters();
  }
}
