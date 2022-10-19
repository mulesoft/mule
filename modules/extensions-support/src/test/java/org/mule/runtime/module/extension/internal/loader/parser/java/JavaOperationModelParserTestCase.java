/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;

import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.property.SinceMuleVersionModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.OperationWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ParameterWrapper;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Optional;

import org.junit.Test;
import org.mule.sdk.api.annotation.Alias;
import org.mule.sdk.api.annotation.MinMuleVersion;

public class JavaOperationModelParserTestCase {

  private JavaOperationModelParser parser;
  private OperationElement operationElement;

  @Test
  public void parseTransactionalOperation() throws NoSuchMethodException {
    parseTransactionalOperation(getMethod());
  }

  @Test
  public void parseSdkTransactionalOperation() throws NoSuchMethodException {
    parseTransactionalOperation(getSdkMethod());
  }

  @Test
  public void calculateMinMuleVersionForOperation() throws NoSuchMethodException {
    parseTransactionalOperation(getMethod());
    Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty = parser.getSinceMuleVersionModelProperty();
    assertThat(sinceMuleVersionModelProperty.isPresent(), is(true));
    assertThat(sinceMuleVersionModelProperty.get().getVersion().toString(), is("4.1.0"));
  }

  @Test
  public void calculateMinMuleVersionForSdkOperation() throws NoSuchMethodException {
    parseTransactionalOperation(getSdkMethod());
    Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty = parser.getSinceMuleVersionModelProperty();
    assertThat(sinceMuleVersionModelProperty.isPresent(), is(true));
    assertThat(sinceMuleVersionModelProperty.get().getVersion().toString(), is("4.5"));
  }

  @Test
  public void operationMinMuleVersionAnnotationTakesPrecedence() throws NoSuchMethodException {
    parseOperation(WithAnnotatedMethod.class.getMethod("annotatedMethod", String.class));
    Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty = parser.getSinceMuleVersionModelProperty();
    assertThat(sinceMuleVersionModelProperty.isPresent(), is(true));
    assertThat(sinceMuleVersionModelProperty.get().getVersion().toString(), is("4.6.1"));
  }

  public void parseTransactionalOperation(Method method) {
    operationElement = new OperationWrapper(method, new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader()));
    ExtensionElement extensionElement = mock(ExtensionElement.class);
    when(extensionElement.getParametersAnnotatedWith(Connection.class))
        .thenReturn(Collections.singletonList(new ParameterWrapper(method, 0, TYPE_LOADER)));
    parser = new JavaOperationModelParser(mock(JavaExtensionModelParser.class), mock(ExtensionElement.class),
                                          mock(OperationContainerElement.class), operationElement,
                                          mock(ExtensionLoadingContext.class));
    assertThat(parser.isTransactional(), is(true));
  }

  public void parseOperation(Method method) {
    operationElement = new OperationWrapper(method, new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader()));
    parser = new JavaOperationModelParser(mock(JavaExtensionModelParser.class), mock(ExtensionElement.class),
                                          mock(OperationContainerElement.class), operationElement,
                                          mock(ExtensionLoadingContext.class));
  }

  public Method getMethod() throws NoSuchMethodException {
    return TransactionalOperations.class.getMethod("transactionalOperation",
                                                   JavaConnectionProviderModelParserTestCase.TestTransactionalConnection.class);
  }

  private class TransactionalOperations {

    public void transactionalOperation(@Connection JavaConnectionProviderModelParserTestCase.TestTransactionalConnection connection) {}
  }

  public Method getSdkMethod() throws NoSuchMethodException {
    return SdkTransactionalOperations.class
        .getMethod("transactionalOperation", JavaConnectionProviderModelParserTestCase.SdkTestTransactionalConnection.class);
  }

  private class SdkTransactionalOperations {

    public void transactionalOperation(@Connection JavaConnectionProviderModelParserTestCase.SdkTestTransactionalConnection connection) {}
  }


  private class WithAnnotatedMethod {

    @MinMuleVersion("4.6.1")
    public void annotatedMethod(@Alias("someAlias") String aliased) {}
  }
}
