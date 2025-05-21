/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.test;

import static org.mule.runtime.extension.api.runtime.source.BackPressureMode.DROP;
import static org.mule.runtime.extension.api.runtime.source.BackPressureMode.FAIL;
import static org.mule.runtime.extension.api.runtime.source.BackPressureMode.WAIT;
import static org.mule.runtime.module.extension.internal.loader.parser.java.test.MinMuleVersionTestUtils.ctxResolvingMinMuleVersion;
import static org.mule.runtime.module.extension.internal.loader.parser.java.utils.ResolvedMinMuleVersion.FIRST_MULE_VERSION;
import static org.mule.sdk.api.annotation.source.SourceClusterSupport.DEFAULT_ALL_NODES;
import static org.mule.sdk.api.annotation.source.SourceClusterSupport.DEFAULT_PRIMARY_NODE_ONLY;
import static org.mule.sdk.api.annotation.source.SourceClusterSupport.NOT_SUPPORTED;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.Mockito.mock;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.source.BackPressure;
import org.mule.runtime.extension.api.annotation.source.ClusterSupport;
import org.mule.runtime.extension.api.annotation.source.OnBackPressure;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.property.BackPressureStrategyModelProperty;
import org.mule.runtime.extension.api.property.SourceClusterSupportModelProperty;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.SourceElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.SourceTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaSourceModelParser;
import org.mule.sdk.api.annotation.MinMuleVersion;
import org.mule.sdk.api.annotation.execution.OnError;
import org.mule.sdk.api.annotation.source.EmitsResponse;
import org.mule.sdk.api.connectivity.ConnectionProvider;
import org.mule.sdk.api.runtime.connectivity.Reconnectable;
import org.mule.sdk.api.runtime.connectivity.ReconnectionCallback;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.parameter.Literal;
import org.mule.sdk.api.runtime.source.BackPressureContext;
import org.mule.sdk.api.runtime.source.PollContext;
import org.mule.sdk.api.runtime.source.PollingSource;
import org.mule.sdk.api.runtime.source.SourceCallbackContext;
import org.mule.sdk.api.runtime.source.SourceCompletionCallback;
import org.mule.sdk.api.runtime.source.SourceResult;
import org.mule.sdk.api.store.ObjectStoreManager;
import org.mule.sdk.api.tx.SourceTransactionalAction;
import org.mule.sdk.compatibility.api.utils.ForwardCompatibilityHelper;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import jakarta.inject.Inject;

public class JavaSourceModelParserTestCase {

  protected JavaSourceModelParser parser;
  protected SourceElement sourceElement;

  @Test
  public void defaultClusterSupport() {
    SourceClusterSupportModelProperty clusterSupport = parseClusterSupportFromSourceClass(TestSource.class);
    assertThat(clusterSupport.getSourceClusterSupport(), is(DEFAULT_ALL_NODES));
  }

  @Test
  public void noClusterSupport() {
    SourceClusterSupportModelProperty clusterSupport = parseClusterSupportFromSourceClass(NonClusteredSource.class);
    assertThat(clusterSupport.getSourceClusterSupport(), is(NOT_SUPPORTED));
  }

  @Test
  public void clusterSupportDefaultingAllNodes() {
    SourceClusterSupportModelProperty clusterSupport = parseClusterSupportFromSourceClass(AllNodesSource.class);
    assertThat(clusterSupport.getSourceClusterSupport(), is(DEFAULT_ALL_NODES));
  }

  @Test
  public void clusterSupportDefaultingPrimaryNodeOnly() {
    SourceClusterSupportModelProperty clusterSupport = parseClusterSupportFromSourceClass(PrimaryNodeSource.class);
    assertThat(clusterSupport.getSourceClusterSupport(), is(DEFAULT_PRIMARY_NODE_ONLY));
  }

  @Test
  public void sdkNoClusterSupport() {
    SourceClusterSupportModelProperty clusterSupport = parseClusterSupportFromSourceClass(SdkNonClusteredSource.class);
    assertThat(clusterSupport.getSourceClusterSupport(), is(NOT_SUPPORTED));
  }

  @Test
  public void sdkClusterSupportDefaultingAllNodes() {
    SourceClusterSupportModelProperty clusterSupport = parseClusterSupportFromSourceClass(SdkAllNodesSource.class);
    assertThat(clusterSupport.getSourceClusterSupport(), is(DEFAULT_ALL_NODES));
  }

  @Test
  public void sdkClusterSupportDefaultingPrimaryNodeOnly() {
    SourceClusterSupportModelProperty clusterSupport = parseClusterSupportFromSourceClass(SdkPrimaryNodeSource.class);
    assertThat(clusterSupport.getSourceClusterSupport(), is(DEFAULT_PRIMARY_NODE_ONLY));
  }

  @Test
  public void noBackPressureStrategy() {
    Optional<BackPressureStrategyModelProperty> backPressureStrategyModelProperty =
        parseBackPressureStrategyFromSourceClass(TestSource.class);
    assertThat(backPressureStrategyModelProperty.isPresent(), is(false));
  }

  @Test
  public void backPressureSourceWithDefaults() {
    Optional<BackPressureStrategyModelProperty> backPressureStrategyModelProperty =
        parseBackPressureStrategyFromSourceClass(BackPressureSourceWithDefaults.class);
    assertThat(backPressureStrategyModelProperty.isPresent(), is(true));
    assertThat(backPressureStrategyModelProperty.get().getDefaultMode(), is(WAIT));
    assertThat(backPressureStrategyModelProperty.get().getSupportedModes(), hasItems(WAIT));
  }

  @Test
  public void sdkBackPressureSourceWithDefaults() {
    Optional<BackPressureStrategyModelProperty> backPressureStrategyModelProperty =
        parseBackPressureStrategyFromSourceClass(SdkBackPressureSourceWithDefaults.class);
    assertThat(backPressureStrategyModelProperty.isPresent(), is(true));
    assertThat(backPressureStrategyModelProperty.get().getDefaultMode(), is(WAIT));
    assertThat(backPressureStrategyModelProperty.get().getSupportedModes(), hasItems(WAIT));
  }

  @Test
  public void backPressureSourceWithSelectedValues() {
    Optional<BackPressureStrategyModelProperty> backPressureStrategyModelProperty =
        parseBackPressureStrategyFromSourceClass(BackPressureSourceWithSelectedValues.class);
    assertThat(backPressureStrategyModelProperty.isPresent(), is(true));
    assertThat(backPressureStrategyModelProperty.get().getDefaultMode(), is(DROP));
    assertThat(backPressureStrategyModelProperty.get().getSupportedModes(), hasItems(FAIL, DROP));
  }

  @Test
  public void sdkBackPressureSourceWithSelectedValues() {
    Optional<BackPressureStrategyModelProperty> backPressureStrategyModelProperty =
        parseBackPressureStrategyFromSourceClass(SdkBackPressureSourceWithSelectedValues.class);
    assertThat(backPressureStrategyModelProperty.isPresent(), is(true));
    assertThat(backPressureStrategyModelProperty.get().getDefaultMode(), is(DROP));
    assertThat(backPressureStrategyModelProperty.get().getSupportedModes(), hasItems(FAIL, DROP));
  }

  @Test
  public void sourceEmitsResponse() {
    assertThat(parseEmitsResponseFromSourceClass(EmitsResponseSource.class), is(true));
  }

  @Test
  public void sdkSourceEmitsResponse() {
    assertThat(parseEmitsResponseFromSourceClass(SdkEmitsResponseSource.class), is(true));
  }

  @Test
  public void sourceDoesNotEmitsResponse() {
    assertThat(parseEmitsResponseFromSourceClass(TestSource.class), is(false));
  }

  @Test
  public void getMMVForSdkApiSource() {
    mockSourceWrapperWithClass(SdkNonClusteredSource.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SdkNonClusteredSource has min mule version 4.5 because it is annotated with ClusterSupport. ClusterSupport was introduced in Mule 4.5."));
  }

  @Test
  public void getMMVForLegacyApiSource() {
    mockSourceWrapperWithClass(TestSource.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion(), is(FIRST_MULE_VERSION));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source TestSource has min mule version 4.1 because it is the default value."));
  }

  @Test
  public void getMMVForSourceWithListResultOutput() {
    mockSourceWrapperWithClass(SourceListResultOutput.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceListResultOutput has min mule version 4.4 because it has a generic of type Result. Result was introduced in Mule 4.4."));
  }

  @Test
  public void getMMVForSourceWithResultOutput() {
    mockSourceWrapperWithClass(SourceResultOutput.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceResultOutput has min mule version 4.4 because it has a generic of type Result. Result was introduced in Mule 4.4."));
  }

  @Test
  public void getMMVForSourceExtendsReconnectable() {
    mockSourceWrapperWithClass(SourceImplementsReconnectable.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceImplementsReconnectable has min mule version 4.5 because it implements interface Reconnectable. Reconnectable was introduced in Mule 4.5."));
  }

  @Test
  public void getMMVForSourceExtendsExtraReconnectable() {
    mockSourceWrapperWithClass(SourceImplementsExtraReconnectable.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceImplementsExtraReconnectable has min mule version 4.5 because it implements interface ExtraReconnectable. Interface ExtraReconnectable has min mule version 4.5 because it implements interface Reconnectable. Reconnectable was introduced in Mule 4.5."));
  }

  @Test
  public void getMMVForSourceWithSdkAnnotation() {
    mockSourceWrapperWithClass(SdkEmitsResponseSource.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SdkEmitsResponseSource has min mule version 4.5 because it is annotated with EmitsResponse. EmitsResponse was introduced in Mule 4.5."));
  }

  @Test
  public void getMMVForSourceWithSdkField() {
    mockSourceWrapperWithClass(SourceWithSdkField.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceWithSdkField has min mule version 4.5 because of its field someField. Field someField has min mule version 4.5 because it is of type Literal. Literal was introduced in Mule 4.5."));
  }

  @Test
  public void getMMVForSourceWithInjectedField() {
    mockSourceWrapperWithClass(SourceWithInjectedSdkField.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceWithInjectedSdkField has min mule version 4.5 because of its field objectStoreManager. Field objectStoreManager has min mule version 4.5 because it is of type ObjectStoreManager. ObjectStoreManager was introduced in Mule 4.5."));
  }

  @Test
  public void getMMVForSourceWithInjectField() {
    mockSourceWrapperWithClass(SourceWithInjectedOptionalSdkField.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.1"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceWithInjectedOptionalSdkField has min mule version 4.1 because it is the default value."));
  }

  @Test
  public void getMMVForSourceWithAutomaticallyInjectedSdkField() {
    mockSourceWrapperWithClass(SourceWithAutomaticallyInjectedSdkField.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceWithAutomaticallyInjectedSdkField has min mule version 4.5 because of its field sourceTransactionalAction. Field sourceTransactionalAction has min mule version 4.5 because it is of type SourceTransactionalAction. SourceTransactionalAction was introduced in Mule 4.5."));
  }

  @Test
  public void getMMVForSourceWithSdkConnectionProvider() {
    mockSourceWrapperWithClass(SourceWithSdkConnectionProvider.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceWithSdkConnectionProvider has min mule version 4.5 because of its field connectionProvider. Field connectionProvider has min mule version 4.5 because it is of type ConnectionProvider. ConnectionProvider was introduced in Mule 4.5."));
  }

  @Test
  public void getMMVForSourceWithSdkParameterField() {
    mockSourceWrapperWithClass(SourceWithSdkParameterField.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceWithSdkParameterField has min mule version 4.4 because of its field someField. Field someField has min mule version 4.4 because it is annotated with Parameter. Parameter was introduced in Mule 4.4."));
  }

  @Test
  public void getMMVForSourceWithSdkInvalidField() {
    mockSourceWrapperWithClass(SourceWithMMVField.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceWithMMVField has min mule version 4.5 because of its field someField. Field someField has min mule version 4.5 because it is annotated with @MinMuleVersion."));
  }

  @Test
  public void getMMVForSourceWithSdkParameterGroup() {
    mockSourceWrapperWithClass(SourceWithSdkParameterGroup.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceWithSdkParameterGroup has min mule version 4.4 because of its field sdkParameterGroup. Field sdkParameterGroup has min mule version 4.4 because it is a parameter of type SdkParameterGroup. Type SdkParameterGroup has min mule version 4.4 because of its field someField. Field someField has min mule version 4.4 because it is annotated with Parameter. Parameter was introduced in Mule 4.4."));
  }

  @Test
  public void getMMVForSourceWithSdkParametersContainer() {
    mockSourceWrapperWithClass(SourceWithSdkParametersContainer.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceWithSdkParametersContainer has min mule version 4.5 because of its field parametersContainer. Field parametersContainer has min mule version 4.5 because it is a parameter of type SdkParametersContainer. Type SdkParametersContainer has min mule version 4.5 because of its field sdkLiteralParameter. Field sdkLiteralParameter has min mule version 4.5 because it is of type Literal. Literal was introduced in Mule 4.5."));
  }

  @Test
  public void getMMVForSourceWithNestedContainer() {
    mockSourceWrapperWithClass(SourceWithNestedContainer.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceWithNestedContainer has min mule version 4.5 because of its field parametersContainer. Field parametersContainer has min mule version 4.5 because it is a parameter of type HasNestedContainer. Type HasNestedContainer has min mule version 4.5 because of its field nestedContainer. Field nestedContainer has min mule version 4.5 because it is a parameter of type SdkParametersContainer. Type SdkParametersContainer has min mule version 4.5 because of its field sdkLiteralParameter. Field sdkLiteralParameter has min mule version 4.5 because it is of type Literal. Literal was introduced in Mule 4.5."));
  }

  @Test
  public void getMMVForSourceWithOnBackPressure() {
    mockSourceWrapperWithClass(SourceOnBackPressure.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceOnBackPressure has min mule version 4.4 because of its method onBackPressure. Method onBackPressure has min mule version 4.4 because of its parameter ctx. Parameter ctx has min mule version 4.4 because it is of type BackPressureContext. BackPressureContext was introduced in Mule 4.4."));
  }

  @Test
  public void getMMVForSourceWithOnSuccess() {
    mockSourceWrapperWithClass(SourceOnSuccess.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceOnSuccess has min mule version 4.4 because of its method onSuccess. Method onSuccess has min mule version 4.4 because of its parameter callback. Parameter callback has min mule version 4.4 because it is of type SourceCompletionCallback. SourceCompletionCallback was introduced in Mule 4.4."));
  }

  @Test
  public void getMMVForSourceWithOnError() {
    mockSourceWrapperWithClass(SourceOnError.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceOnError has min mule version 4.5 because of its method onError. Method onError has min mule version 4.5 because it is annotated with OnError. OnError was introduced in Mule 4.5."));
  }

  @Test
  public void getMMVForSourceWithOnTerminate() {
    mockSourceWrapperWithClass(SourceOnTerminate.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceOnTerminate has min mule version 4.4 because of its method onTerminate. Method onTerminate has min mule version 4.4 because of its parameter sourceResult. Parameter sourceResult has min mule version 4.4 because it is of type SourceResult. SourceResult was introduced in Mule 4.4."));
  }

  @Test
  public void getMMVForSourceWithNonAnnotatedMethod() {
    mockSourceWrapperWithClass(SourceWithNonAnnotatedMethod.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceWithNonAnnotatedMethod has min mule version 4.5 because of its method someMethod. Method someMethod has min mule version 4.5 because of its parameter param. Parameter param has min mule version 4.5 because it is of type Literal. Literal was introduced in Mule 4.5."));
  }

  @Test
  public void getClassLevelMMVForSourceWithMMVAnnotation() {
    mockSourceWrapperWithClass(SourceWithHigherMMVAnnotation.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.7"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceWithHigherMMVAnnotation has min mule version 4.7 because it is the one set at the class level through the @MinMuleVersion annotation."));
  }

  @Test
  public void getOverwrittenMMVForSourceWithMMVAnnotation() {
    mockSourceWrapperWithClass(SourceWithLowerMMVAnnotation.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.5"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Calculated Min Mule Version is 4.5 which is greater than the one set at the source class level 4.4. Overriding it. Source SourceWithLowerMMVAnnotation has min mule version 4.5 because of its field sourceTransactionalAction. Field sourceTransactionalAction has min mule version 4.5 because it is of type SourceTransactionalAction. SourceTransactionalAction was introduced in Mule 4.5."));
  }

  @Test
  public void getMMVForExtendsPollingSource() {
    mockSourceWrapperWithClass(ExtendsPollingSource.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source ExtendsPollingSource has min mule version 4.4 because of its super class PollingSource. Source PollingSource has min mule version 4.4 because it is the one set at the class level through the @MinMuleVersion annotation."));
  }

  @Test
  public void getMMVForSourceWithRecursiveField() {
    mockSourceWrapperWithClass(SourceWithRecursiveParameter.class);
    assertThat(parser.getResolvedMinMuleVersion().get().getMinMuleVersion().toString(), is("4.4"));
    assertThat(parser.getResolvedMinMuleVersion().get().getReason(),
               is("Source SourceWithRecursiveParameter has min mule version 4.4 because of its field recursiveParameter. Field recursiveParameter has min mule version 4.4 because it is a parameter of type RecursiveClass. Type RecursiveClass has min mule version 4.4 because of its field recursiveField. Field recursiveField has min mule version 4.4 because it is annotated with Optional. Optional was introduced in Mule 4.4."));
  }

  private boolean parseEmitsResponseFromSourceClass(Class<? extends Source> sourceClass) {
    mockSourceWrapperWithClass(sourceClass);
    return parser.emitsResponse();
  }

  private Optional<BackPressureStrategyModelProperty> parseBackPressureStrategyFromSourceClass(Class<? extends Source> sourceClass) {
    mockSourceWrapperWithClass(sourceClass);
    return parser.getBackPressureStrategyModelProperty();
  }

  private SourceClusterSupportModelProperty parseClusterSupportFromSourceClass(Class<? extends Source> sourceClass) {
    mockSourceWrapperWithClass(sourceClass);
    return parser.getSourceClusterSupportModelProperty();
  }

  protected void mockSourceWrapperWithClass(Class sourceClass) {
    sourceElement = new SourceTypeWrapper<>(sourceClass, new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader()));
    parser = new JavaSourceModelParser(mock(ExtensionElement.class), sourceElement, ctxResolvingMinMuleVersion());
  }

  public static class TestSource extends Source<String, Object> {

    @Override
    public void onStart(SourceCallback<String, Object> sourceCallback) throws MuleException {

    }

    @Override
    public void onStop() {

    }
  }

  @ClusterSupport(org.mule.runtime.extension.api.annotation.source.SourceClusterSupport.NOT_SUPPORTED)
  public static class NonClusteredSource extends TestSource {

  }

  @ClusterSupport(org.mule.runtime.extension.api.annotation.source.SourceClusterSupport.DEFAULT_ALL_NODES)
  public static class AllNodesSource extends TestSource {

  }

  @ClusterSupport(org.mule.runtime.extension.api.annotation.source.SourceClusterSupport.DEFAULT_PRIMARY_NODE_ONLY)
  public static class PrimaryNodeSource extends TestSource {

  }

  @org.mule.sdk.api.annotation.source.ClusterSupport(NOT_SUPPORTED)
  public static class SdkNonClusteredSource extends TestSource {

  }

  @org.mule.sdk.api.annotation.source.ClusterSupport(DEFAULT_ALL_NODES)
  public static class SdkAllNodesSource extends TestSource {

  }

  @org.mule.sdk.api.annotation.source.ClusterSupport(DEFAULT_PRIMARY_NODE_ONLY)
  public static class SdkPrimaryNodeSource extends TestSource {

  }

  @BackPressure()
  public static class BackPressureSourceWithDefaults extends TestSource {

  }

  @org.mule.sdk.api.annotation.source.BackPressure()
  public static class SdkBackPressureSourceWithDefaults extends TestSource {

  }

  @BackPressure(defaultMode = DROP, supportedModes = {FAIL, DROP})
  public static class BackPressureSourceWithSelectedValues extends TestSource {

  }

  @org.mule.sdk.api.annotation.source.BackPressure(defaultMode = org.mule.sdk.api.runtime.source.BackPressureMode.DROP,
      supportedModes = {org.mule.sdk.api.runtime.source.BackPressureMode.FAIL,
          org.mule.sdk.api.runtime.source.BackPressureMode.DROP})
  public static class SdkBackPressureSourceWithSelectedValues extends TestSource {

  }

  @org.mule.runtime.extension.api.annotation.source.EmitsResponse
  public static class EmitsResponseSource extends TestSource {

  }

  @EmitsResponse
  public static class SdkEmitsResponseSource extends TestSource {

  }

  private static class SourceListResultOutput extends Source<List<Result<String, String>>, String> {

    @Override
    public void onStart(SourceCallback<List<Result<String, String>>, String> sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }

  private static class SourceResultOutput extends Source<Result<String, String>, String> {

    @Override
    public void onStart(SourceCallback<Result<String, String>, String> sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }

  private static class SourceImplementsReconnectable extends Source<String, String> implements Reconnectable {

    @Override
    public void onStart(SourceCallback<String, String> sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}

    @Override
    public void reconnect(ConnectionException exception, ReconnectionCallback reconnectionCallback) {}
  }

  private interface ExtraReconnectable extends Reconnectable {
  }

  private static class SourceImplementsExtraReconnectable extends Source<String, String> implements ExtraReconnectable {

    @Override
    public void onStart(SourceCallback<String, String> sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}

    @Override
    public void reconnect(ConnectionException exception, ReconnectionCallback reconnectionCallback) {}
  }

  private static class SourceWithSdkField extends TestSource {

    Literal<String> someField;
  }

  private static class SourceWithMMVField extends TestSource {

    @MinMuleVersion("4.5.0")
    String someField;
  }

  private static class SourceWithInjectedSdkField extends TestSource {

    @Inject
    ObjectStoreManager objectStoreManager;
  }

  private static class SourceWithInjectedOptionalSdkField extends TestSource {

    @Inject
    private Optional<ForwardCompatibilityHelper> forwardCompatibilityHelper;
  }

  private static class SourceWithAutomaticallyInjectedSdkField extends TestSource {

    SourceTransactionalAction sourceTransactionalAction;
  }

  private static class SourceWithSdkConnectionProvider extends TestSource {

    @Connection
    ConnectionProvider<String> connectionProvider;
  }

  private static class SourceWithSdkParameterField extends TestSource {

    @org.mule.sdk.api.annotation.param.Parameter
    String someField;
  }

  private static class SourceWithSdkParameterGroup extends TestSource {

    @ParameterGroup(name = "pg")
    SdkParameterGroup sdkParameterGroup;
  }

  private static class SourceWithSdkParametersContainer extends TestSource {

    @Parameter
    SdkParametersContainer parametersContainer;
  }

  private static class SourceWithNestedContainer extends TestSource {

    @Parameter
    HasNestedContainer parametersContainer;
  }

  private static class SourceOnBackPressure extends TestSource {

    @OnBackPressure
    public void onBackPressure(BackPressureContext ctx) {}
  }

  private static class SourceOnSuccess extends TestSource {

    @OnSuccess
    public void onSuccess(SourceCompletionCallback callback) {}
  }

  private static class SourceOnError extends TestSource {

    @OnError
    public void onError() {}
  }

  private static class SourceOnTerminate extends TestSource {

    @OnTerminate
    public void onTerminate(SourceResult sourceResult) {}
  }

  private static class SourceWithNonAnnotatedMethod extends TestSource {

    public Result<String, String> someMethod(Literal<String> param) {
      return Result.<String, String>builder().output(param.getLiteralValue().orElse("")).attributes("Att").build();
    }
  }

  @MinMuleVersion("4.7")
  private static class SourceWithHigherMMVAnnotation extends TestSource {

    SourceTransactionalAction sourceTransactionalAction;
  }

  @MinMuleVersion("4.4")
  private static class SourceWithLowerMMVAnnotation extends TestSource {

    SourceTransactionalAction sourceTransactionalAction;
  }

  private static class SdkParameterGroup {

    @org.mule.sdk.api.annotation.param.Parameter
    String someField;

    @org.mule.sdk.api.annotation.param.Parameter
    String anotherField;
  }

  private static class SdkParametersContainer {

    @Parameter
    Literal<String> sdkLiteralParameter;

    @Parameter
    String anotherField;
  }

  private static class HasNestedContainer {

    @Parameter
    SdkParametersContainer nestedContainer;
  }

  private static class ExtendsPollingSource extends PollingSource<String, String> {

    @Override
    protected void doStart() throws MuleException {

    }

    @Override
    protected void doStop() {

    }

    @Override
    public void poll(PollContext<String, String> pollContext) {

    }

    @Override
    public void onRejectedItem(Result<String, String> result, SourceCallbackContext callbackContext) {

    }
  }

  private static class SourceWithRecursiveParameter extends TestSource {

    @Parameter
    RecursiveClass recursiveParameter;
  }

  private static class RecursiveClass {

    @Parameter
    String someField;

    @Parameter
    @org.mule.sdk.api.annotation.param.Optional
    RecursiveClass recursiveField;
  }
}
