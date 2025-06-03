/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.test;

import static org.mule.runtime.module.extension.internal.loader.parser.java.test.MinMuleVersionTestUtils.ctxResolvingMinMuleVersion;
import static org.mule.runtime.module.extension.internal.loader.parser.java.utils.ResolvedMinMuleVersion.FIRST_MULE_VERSION;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;

import static org.mockito.Mockito.mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.values.ValuePart;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.OperationWrapper;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaOperationModelParser;
import org.mule.sdk.api.annotation.Alias;
import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.MinMuleVersion;
import org.mule.sdk.api.annotation.Operations;
import org.mule.sdk.api.annotation.param.Parameter;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.parameter.CorrelationInfo;
import org.mule.sdk.api.runtime.parameter.Literal;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

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
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion(), is(FIRST_MULE_VERSION));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Operation transactionalOperation has min mule version 4.1 because it is the default value."));
  }

  @Test
  public void getMMVForOperationAnnotatedWithMMV() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "annotatedWithMMV");
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Method annotatedWithMMV has min mule version 4.4 because it is the one set at the method level through the @MinMuleVersion annotation."));
  }

  @Test
  public void getOverwrittenMMVForOperationAnnotatedWithMMV() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "overwriteMMV");
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Calculated Min Mule Version is 4.5 which is greater than the one set at the method level 4.4. Overriding it. Method overwriteMMV has min mule version 4.5 because it is annotated with Alias. Alias was introduced in Mule 4.5."));
  }

  @Test
  public void getMMVForOperationWithSdkParameter() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withSdkParameter", Literal.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Method withSdkParameter has min mule version 4.5 because of its parameter literalParameter. Parameter literalParameter has min mule version 4.5 because it is of type Literal. Literal was introduced in Mule 4.5."));
  }

  @Test
  public void getMMVForOperationWithSdkImplicitParameter() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withSdkImplicitParameter", CorrelationInfo.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Method withSdkImplicitParameter has min mule version 4.5 because of its parameter info. Parameter info has min mule version 4.5 because it is of type CorrelationInfo. CorrelationInfo was introduced in Mule 4.5."));
  }

  @Test
  public void getMMVForOperationWithSdkAnnotatedParameter() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withSdkAnnotatedParameter", String.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(), is(getMessageForOperationWithSdkAnnotatedParameter()));
  }

  @Test
  public void getMMVForOperationWithParameterGroup() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withParameterGroup", ParameterGroup.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(), is(getMessageForOperationWithParameterGroup()));
  }

  @Test
  public void getMMVForOperationWithParameterGroupWithValueAnnotation() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withGroupAsMultiLevelValue", GroupAsMultiLevelValue.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is(getMessageForOperationWithParameterGroupWithValueAnnotation()));
  }

  @Test
  public void getMMVForOperationWithParameterGroupWithLiteralField() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withGroupWithLiteralField",
                   ParameterGroupWithLiteralField.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5.0"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is(getMessageForOperationWithParameterGroupWithLiteralField()));
  }

  @Test
  public void getMMVForOperationWithParameterContainer() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withParameterContainer", ParameterGroup.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Method withParameterContainer has min mule version 4.4 because of its parameter parameterContainer. Parameter parameterContainer has min mule version 4.4 because it is of type ParameterGroup. Type ParameterGroup has min mule version 4.4 because of its field someField. Field someField has min mule version 4.4 because it is annotated with Parameter. Parameter was introduced in Mule 4.4."));
  }

  @Test
  public void getMMVForOperationWithConfigParameter() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withConfigParameter", SomeConfiguration.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(), is(getMessageOperationWithConfigParameter()));
  }

  @Test
  public void getMMVForOperationWithSdkPagingProvider() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withSdkPagingProvider");
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Method withSdkPagingProvider has min mule version 4.4 because of its output type PagingProvider. PagingProvider was introduced in Mule 4.4."));
  }

  @Test
  public void getMMVForOperationWithPagingProviderSdkGeneric() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withPagingProviderSdkGeneric");
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Method withPagingProviderSdkGeneric has min mule version 4.4 because of its output type Result. Result was introduced in Mule 4.4."));
  }

  @Test
  public void getMMVForOperationWithResultOutput() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withResultOutput");
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Method withResultOutput has min mule version 4.4 because of its output type Result. Result was introduced in Mule 4.4."));
  }

  @Test
  public void getMMVLegacyApiTransactionalOperation() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), TransactionalOperations.class, "transactionalOperation",
                   JavaConnectionProviderModelParserTestCase.TestTransactionalConnection.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.1"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Operation transactionalOperation has min mule version 4.1 because it is the default value."));
  }

  @Test
  public void getMMVLegacySdkApiTransactionalOperation() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SdkTransactionalOperations.class, "transactionalOperation",
                   JavaConnectionProviderModelParserTestCase.SdkTestTransactionalConnection.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion(), is(FIRST_MULE_VERSION));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Operation transactionalOperation has min mule version 4.1 because it is the default value."));
  }

  @Test
  public void getMMVForParameterizedOperationsContainer() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), ParameterizedOperations.class, "noArgumentsOperation");
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Operation noArgumentsOperation has min mule version 4.4 because of its parameter containerParameter. Parameter containerParameter has min mule version 4.4 because it is annotated with Parameter. Parameter was introduced in Mule 4.4."));
  }

  @Test
  public void getMMVForOperationFromConfigurationWithSdkOperationsAnnotation() throws NoSuchMethodException {
    parseOperation(getExtensionElement(ConfigurationWithSdkOperationsAnnotation.class), TransactionalOperations.class,
                   "transactionalOperation", JavaConnectionProviderModelParserTestCase.TestTransactionalConnection.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Operation transactionalOperation has min mule version 4.4 because it was propagated from the @Operations annotation at the extension class used to add the operation's container TransactionalOperations."));
  }

  @Test
  public void getMMVForOperationWithArrayListOutput() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withArrayListOutput");
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Method withArrayListOutput has min mule version 4.5 because of its output type Literal. Literal was introduced in Mule 4.5."));
  }

  @Test
  public void getMMVForOperationWithNativeArrayOutput() throws NoSuchMethodException {
    parseOperation(mock(ExtensionElement.class), SkdOperations.class, "withNativeArrayOutput");
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Method withNativeArrayOutput has min mule version 4.4 because of its output type Result. Result was introduced in Mule 4.4."));
  }

  public void parseOperation(ExtensionElement extensionElement, Class<?> operationClass, String methodName,
                             Class<?>... parameterType)
      throws NoSuchMethodException {
    Method method = operationClass.getMethod(methodName, parameterType);
    operationElement = new OperationWrapper(method, new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader()));
    parser = new JavaOperationModelParser(mock(JavaExtensionModelParser.class), extensionElement,
                                          mock(OperationContainerElement.class), operationElement,
                                          ctxResolvingMinMuleVersion());
  }

  protected ExtensionElement getExtensionElement(Class<?> extensionClass) {
    return new ExtensionTypeWrapper<>(extensionClass, TYPE_LOADER);
  }

  protected String getMessageForOperationWithSdkAnnotatedParameter() {
    return "Method withSdkAnnotatedParameter has min mule version 4.4 because of its parameter optionalParameter. Parameter optionalParameter has min mule version 4.4 because it is annotated with Optional. Optional was introduced in Mule 4.4.";
  }

  protected String getMessageOperationWithConfigParameter() {
    return "Method withConfigParameter has min mule version 4.4 because of its parameter configParameter. Parameter configParameter has min mule version 4.4 because it references a config of type SomeConfiguration. Configuration SomeConfiguration has min mule version 4.4 because of its field configField. Field configField has min mule version 4.4 because it is annotated with Parameter. Parameter was introduced in Mule 4.4.";
  }

  protected String getMessageForOperationWithParameterGroup() {
    return "Method withParameterGroup has min mule version 4.4 because of its parameter parameterGroup. Parameter parameterGroup has min mule version 4.4 because it is of type ParameterGroup. Type ParameterGroup has min mule version 4.4 because of its field someField. Field someField has min mule version 4.4 because it is annotated with Parameter. Parameter was introduced in Mule 4.4.";
  }

  protected String getMessageForOperationWithParameterGroupWithLiteralField() {
    return "Method withGroupWithLiteralField has min mule version 4.5 because of its parameter parameterGroup. Parameter parameterGroup has min mule version 4.5 because it is of type ParameterGroupWithLiteralField. Type ParameterGroupWithLiteralField has min mule version 4.5 because of its field anotherField. Field anotherField has min mule version 4.5 because it is of type Literal. Literal was introduced in Mule 4.5.";
  }

  protected String getMessageForOperationWithParameterGroupWithValueAnnotation() {
    return "Method withGroupAsMultiLevelValue has min mule version 4.5 because of its parameter parameterGroup. Parameter parameterGroup has min mule version 4.5 because it is of type GroupAsMultiLevelValue. Type GroupAsMultiLevelValue has min mule version 4.5 because of its field country. Field country has min mule version 4.5 because it is annotated with ValuePart. ValuePart was introduced in Mule 4.5.";
  }

  private class TransactionalOperations {

    public void transactionalOperation(@Connection JavaConnectionProviderModelParserTestCase.TestTransactionalConnection connection) {}
  }

  private static class SdkTransactionalOperations {

    public void transactionalOperation(@Connection JavaConnectionProviderModelParserTestCase.SdkTestTransactionalConnection connection) {}
  }

  /**
   * Also see {@code testfiles.SdkOperations} in {@code mule-extensions-ast-loader}.
   */
  private static class SkdOperations {

    @MinMuleVersion("4.4")
    public void annotatedWithMMV() {}

    @MinMuleVersion("4.4")
    @Alias("operation alias")
    public void overwriteMMV() {}

    public void withSdkParameter(Literal<String> literalParameter) {}

    public void withSdkImplicitParameter(CorrelationInfo info) {}

    public void withSdkAnnotatedParameter(@org.mule.sdk.api.annotation.param.Optional String optionalParameter) {}

    public void withParameterGroup(@org.mule.runtime.extension.api.annotation.param.ParameterGroup(
        name = "pg") JavaOperationModelParserTestCase.ParameterGroup parameterGroup) {}

    public void withParameterContainer(ParameterGroup parameterContainer) {}

    public void withGroupAsMultiLevelValue(@org.mule.runtime.extension.api.annotation.param.ParameterGroup(
        name = "pg") GroupAsMultiLevelValue parameterGroup) {}

    public void withGroupWithLiteralField(@org.mule.runtime.extension.api.annotation.param.ParameterGroup(
        name = "pg") ParameterGroupWithLiteralField parameterGroup) {}

    public org.mule.sdk.api.runtime.streaming.PagingProvider<String, String> withSdkPagingProvider() {
      return new org.mule.sdk.api.runtime.streaming.PagingProvider<>() {

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
      return new PagingProvider<>() {

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

    public ArrayList<Literal<String>> withArrayListOutput() {
      return new ArrayList<>();
    }

    public Result<String, Void>[] withNativeArrayOutput() {
      return new Result[1];
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

  @Extension(name = "Configuration With Sdk Operations Annotation")
  @Operations(TransactionalOperations.class)
  private static class ConfigurationWithSdkOperationsAnnotation {
  }

  public static class ParameterGroup {

    @org.mule.sdk.api.annotation.param.Parameter
    String someField;

    @org.mule.sdk.api.annotation.param.Parameter
    String anotherField;
  }

  public static class ParameterGroupWithLiteralField {

    @Parameter
    String someField;

    @Parameter
    Literal<String> anotherField;
  }

  public static class GroupAsMultiLevelValue {

    @Parameter
    @ValuePart(order = 1)
    private String continent;

    @Parameter
    @org.mule.sdk.api.annotation.values.ValuePart(order = 2)
    private String country;
  }
}
