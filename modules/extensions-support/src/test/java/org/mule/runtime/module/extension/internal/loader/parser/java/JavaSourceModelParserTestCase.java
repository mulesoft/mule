/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;


import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.extension.api.runtime.source.BackPressureMode.WAIT;
import static org.mule.runtime.extension.api.runtime.source.BackPressureMode.FAIL;
import static org.mule.runtime.extension.api.runtime.source.BackPressureMode.DROP;
import static org.mule.sdk.api.annotation.source.SourceClusterSupport.DEFAULT_ALL_NODES;
import static org.mule.sdk.api.annotation.source.SourceClusterSupport.DEFAULT_PRIMARY_NODE_ONLY;
import static org.mule.sdk.api.annotation.source.SourceClusterSupport.NOT_SUPPORTED;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.source.BackPressure;
import org.mule.runtime.extension.api.annotation.source.ClusterSupport;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.property.SinceMuleVersionModelProperty;
import org.mule.runtime.extension.api.property.SourceClusterSupportModelProperty;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.property.BackPressureStrategyModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.SourceElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.SourceTypeWrapper;
import org.mule.sdk.api.annotation.source.EmitsResponse;

import java.util.Optional;

import org.junit.Test;

public class JavaSourceModelParserTestCase {

  private JavaSourceModelParser parser;
  private SourceElement sourceElement;

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
  public void sdkSourceMinMuleVersion() {
    mockSourceWrapperWithClass(SdkNonClusteredSource.class);
    Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty = parser.getSinceMuleVersionModelProperty();
    assertThat(sinceMuleVersionModelProperty.isPresent(), is(true));
    assertThat(sinceMuleVersionModelProperty.get().getVersion().toString(), is("4.5.0"));
  }

  @Test
  public void sourceMinMuleVersion() {
    mockSourceWrapperWithClass(TestSource.class);
    Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty = parser.getSinceMuleVersionModelProperty();
    assertThat(sinceMuleVersionModelProperty.isPresent(), is(true));
    assertThat(sinceMuleVersionModelProperty.get().getVersion().toString(), is("4.1.0"));
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

  private void mockSourceWrapperWithClass(Class<? extends Source> sourceClass) {
    sourceElement = new SourceTypeWrapper<>(sourceClass, new DefaultExtensionsTypeLoaderFactory()
        .createTypeLoader(Thread.currentThread().getContextClassLoader()));
    parser = new JavaSourceModelParser(mock(ExtensionElement.class), sourceElement, mock(ExtensionLoadingContext.class));
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

}
