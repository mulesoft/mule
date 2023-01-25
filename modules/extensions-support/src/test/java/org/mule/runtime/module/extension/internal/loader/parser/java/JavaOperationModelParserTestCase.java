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
import static org.mule.runtime.module.extension.internal.loader.parser.java.utils.JavaParserUtils.FIRST_MULE_VERSION;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.OperationWrapper;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.mule.sdk.api.annotation.Alias;
import org.mule.sdk.api.annotation.MinMuleVersion;
import org.mule.sdk.api.annotation.Operations;
import org.mule.sdk.api.annotation.param.Parameter;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.parameter.CorrelationInfo;
import org.mule.sdk.api.runtime.parameter.Literal;

public class JavaOperationModelParserTestCase {

  protected JavaOperationModelParser parser;
  protected OperationElement operationElement;

  @Test
  public void parseTransactionalOperation() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), TransactionalOperations.class, "transactionalOperation",
                   JavaConnectionProviderModelParserTestCase.TestTransactionalConnection.class);
    assertThat(parser.isTransactional(), is(true));
  }

  @Test
  public void parseSdkTransactionalOperation() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SdkTransactionalOperations.class, "transactionalOperation",
                   JavaConnectionProviderModelParserTestCase.SdkTestTransactionalConnection.class);
    assertThat(parser.isTransactional(), is(true));
  }

  @Test
  public void getMMVForVanillaOperation() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), TransactionalOperations.class, "transactionalOperation",
                   JavaConnectionProviderModelParserTestCase.TestTransactionalConnection.class);
    assertThat(parser.isTransactional(), is(true));
    Optional<MuleVersion> minMuleVersion = parser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get(), is(FIRST_MULE_VERSION));
  }

  @Test
  public void getMMVForOperationAnnotatedWithMMV() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "annotatedWithMMV");
    Optional<MuleVersion> minMuleVersion = parser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.4"));
  }

  @Test
  public void getOverwrittenMMVForOperationAnnotatedWithMMV() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "overwriteMMV");
    Optional<MuleVersion> minMuleVersion = parser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.5.0"));
  }

  @Test
  public void getMMVForOperationWithSdkParameter() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withSdkParameter", Literal.class);
    Optional<MuleVersion> minMuleVersion = parser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.5.0"));
  }

  @Test
  public void getMMVForOperationWithSdkImplicitParameter() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withSdkImplicitParameter", CorrelationInfo.class);
    Optional<MuleVersion> minMuleVersion = parser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.5.0"));
  }

  @Test
  public void getMMVForOperationWithSdkAnnotatedParameter() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withSdkAnnotatedParameter", String.class);
    Optional<MuleVersion> minMuleVersion = parser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.4"));
  }

  @Test
  public void getMMVForOperationWithParameterGroup() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withParameterGroup", SdkParameterGroup.class);
    Optional<MuleVersion> minMuleVersion = parser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.4"));
  }

  @Test
  public void getMMVForOperationWithConfigParameter() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withConfigParameter", SomeConfiguration.class);
    Optional<MuleVersion> minMuleVersion = parser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.4"));
  }

  @Test
  public void getMMVForOperationWithSdkPagingProvider() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withSdkPagingProvider");
    Optional<MuleVersion> minMuleVersion = parser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.4"));
  }

  @Test
  public void getMMVForOperationWithPagingProviderSdkGeneric() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withPagingProviderSdkGeneric");
    Optional<MuleVersion> minMuleVersion = parser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.4"));
  }

  @Test
  public void getMMVForOperationWithResultOutput() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withResultOutput");
    Optional<MuleVersion> minMuleVersion = parser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.4"));
  }

  @Test
  public void getMMVLegacyApiTransactionalOperation() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), TransactionalOperations.class, "transactionalOperation",
                   JavaConnectionProviderModelParserTestCase.TestTransactionalConnection.class);
    Optional<MuleVersion> minMuleVersion = parser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.1.1"));
  }

  @Test
  public void getMMVLegacySdkApiTransactionalOperation() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SdkTransactionalOperations.class, "transactionalOperation",
                   JavaConnectionProviderModelParserTestCase.SdkTestTransactionalConnection.class);
    Optional<MuleVersion> minMuleVersion = parser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get(), is(FIRST_MULE_VERSION));
  }

  @Test
  public void getMMVForParameterizedOperationsContainer() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), ParameterizedOperations.class, "noArgumentsOperation");
    Optional<MuleVersion> minMuleVersion = parser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.4"));
  }

  @Test
  public void getMMVForOperationFromConfigurationWithSdkOperationsAnnotation() throws NoSuchMethodException {
    parseOperation(getExtensionElement(ConfigurationWithSdkOperationsAnnotation.class), TransactionalOperations.class,
                   "transactionalOperation", JavaConnectionProviderModelParserTestCase.TestTransactionalConnection.class);
    Optional<MuleVersion> minMuleVersion = parser.getMinMuleVersion();
    assertThat(minMuleVersion.isPresent(), is(true));
    assertThat(minMuleVersion.get().toString(), is("4.4.0"));
  }

  public void parseOperation(ExtensionElement extensionElement, Class<?> operationClass, String methodName,
                             Class<?>... parameterType)
      throws NoSuchMethodException {
    Method method = operationClass.getMethod(methodName, parameterType);
    operationElement = new OperationWrapper(method, new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader()));
    parser = new JavaOperationModelParser(mock(JavaExtensionModelParser.class), extensionElement,
                                          mock(OperationContainerElement.class), operationElement,
                                          mock(ExtensionLoadingContext.class));
  }

  protected ExtensionElement getExtensionElement(Class<?> extensionClass) {
    return new ExtensionTypeWrapper<>(extensionClass, TYPE_LOADER);
  }

  private class TransactionalOperations {

    public void transactionalOperation(@Connection JavaConnectionProviderModelParserTestCase.TestTransactionalConnection connection) {}
  }

  private static class SdkTransactionalOperations {

    public void transactionalOperation(@Connection JavaConnectionProviderModelParserTestCase.SdkTestTransactionalConnection connection) {}
  }

  private static class SkdOperations {

    @MinMuleVersion("4.4")
    public void annotatedWithMMV() {

    }

    @MinMuleVersion("4.4")
    @Alias("operation alias")
    public void overwriteMMV() {

    }

    public void withSdkParameter(Literal<String> literalParameter) {

    }

    public void withSdkImplicitParameter(CorrelationInfo info) {

    }

    public void withSdkAnnotatedParameter(@org.mule.sdk.api.annotation.param.Optional String optionalParameter) {

    }

    public void withParameterGroup(@ParameterGroup(name = "pg") SdkParameterGroup parameterGroup) {

    }

    public org.mule.sdk.api.runtime.streaming.PagingProvider<String, String> withSdkPagingProvider() {
      return new org.mule.sdk.api.runtime.streaming.PagingProvider<String, String>() {

        @Override
        public List<String> getPage(String connection) {
          return null;
        }

        @Override
        public Optional<Integer> getTotalResults(String connection) {
          return Optional.empty();
        }

        @Override
        public void close(String connection) throws MuleException {

        }
      };
    }

    public PagingProvider<String, Result<String, Void>> withPagingProviderSdkGeneric() {
      return new PagingProvider<String, Result<String, Void>>() {

        @Override
        public List<Result<String, Void>> getPage(String connection) {
          return null;
        }

        @Override
        public Optional<Integer> getTotalResults(String connection) {
          return Optional.empty();
        }

        @Override
        public void close(String connection) throws MuleException {

        }
      };
    }

    public Result<String, Void> withResultOutput() {
      return Result.<String, Void>builder().build();
    }

    public void withConfigParameter(@Config SomeConfiguration configParameter) {

    }
  }

  private static class ParameterizedOperations {

    @Parameter
    String containerParameter;

    public void noArgumentsOperation() {}
  }

  private static class SomeConfiguration {

    @org.mule.sdk.api.annotation.param.Parameter
    String configField;
  }

  @Operations(TransactionalOperations.class)
  private static class ConfigurationWithSdkOperationsAnnotation {
  }

  public static class SdkParameterGroup {

    @org.mule.sdk.api.annotation.param.Parameter
    String someField;

    @org.mule.sdk.api.annotation.param.Parameter
    String anotherField;
  }
}
