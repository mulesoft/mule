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
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.reflections.ReflectionUtils.getMethods;
import static org.reflections.ReflectionUtils.withName;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class MimeTypeParametersDeclarationEnricherTestCase extends AbstractMuleTestCase {

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

    when(operation.getParameterGroup(DEFAULT_GROUP_NAME)).thenReturn(new ParameterGroupDeclaration(DEFAULT_GROUP_NAME));
    when(source.getParameterGroup(DEFAULT_GROUP_NAME)).thenReturn(new ParameterGroupDeclaration(DEFAULT_GROUP_NAME));

    when(operation.getModelProperty(ImplementingTypeModelProperty.class)).thenReturn(empty());
    when(source.getModelProperty(ImplementingTypeModelProperty.class)).thenReturn(empty());
    when(source.getSuccessCallback()).thenReturn(empty());
    when(source.getErrorCallback()).thenReturn(empty());
    when(operation.getModelProperty(ImplementingMethodModelProperty.class)).thenReturn(empty());
  }

  @Test
  public void objectReturnTypeOperation() {
    setOperationImplementingMethod("objectMethod");
    enricher.enrich(extensionLoadingContext);
    assertMimeTypeParams(operation);
  }

  @Test
  public void inputStreamReturnTypeOperation() {
    setOperationImplementingMethod("inputStreamMethod");
    enricher.enrich(extensionLoadingContext);
    assertMimeTypeParams(operation);
  }

  @Test
  public void resultObjectReturnTypeOperation() {
    setOperationImplementingMethod("resultString");
    enricher.enrich(extensionLoadingContext);
    assertMimeTypeParams(operation);
  }

  @Test
  public void voidOperation() {
    setOperationImplementingMethod("voidMethod");
    enricher.enrich(extensionLoadingContext);
    assertThat(operation.getAllParameters(), hasSize(0));
  }

  @Test
  public void appleReturnTypeOperation() {
    setOperationImplementingMethod("appleMethod");
    enricher.enrich(extensionLoadingContext);
    assertThat(operation.getAllParameters(), hasSize(0));
  }

  @Test
  public void appleResultReturnTypeOperation() {
    setOperationImplementingMethod("resultApple");
    enricher.enrich(extensionLoadingContext);
    assertThat(operation.getAllParameters(), hasSize(0));
  }

  @Test
  public void stringReturnTypeSource() {
    setSourceImplementingType(TestStringSource.class);
    enricher.enrich(extensionLoadingContext);
    assertMimeTypeParams(source);
  }

  @Test
  public void noGenericsSource() {
    setSourceImplementingType(TestNoGenericsSource.class);
    enricher.enrich(extensionLoadingContext);
    assertMimeTypeParams(source);
  }

  @Test
  public void appleReturnTypeSource() {
    setSourceImplementingType(TestAppleSource.class);
    enricher.enrich(extensionLoadingContext);
    assertThat(source.getAllParameters(), hasSize(0));
  }

  protected void setSourceImplementingType(Class<?> value) {
    when(source.getModelProperty(ImplementingTypeModelProperty.class))
        .thenReturn(Optional.of(new ImplementingTypeModelProperty(value)));
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

  private void setOperationImplementingMethod(String methodName) {
    Method method = getMethods(TestMethods.class, withName(methodName)).stream().findFirst().get();
    when(operation.getModelProperty(ImplementingMethodModelProperty.class))
        .thenReturn(Optional.of(new ImplementingMethodModelProperty(method)));
  }

  public class TestMethods {

    public void voidMethod() {}

    public Result<String, Attributes> resultString() {
      return null;
    }

    public Result<Apple, Attributes> resultApple() {
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
  }


  public class TestStringSource extends Source<String, Attributes> {

    @Override
    public void onStart(SourceCallback<String, Attributes> sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }

  public class TestAppleSource extends Source<Apple, Attributes> {

    @Override
    public void onStart(SourceCallback<Apple, Attributes> sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }

  public class TestNoGenericsSource extends Source {

    @Override
    public void onStart(SourceCallback sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }
}
