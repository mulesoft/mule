/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.EXTENSION_TYPE;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.VERSION;
import static org.mule.sdk.api.meta.Category.SELECT;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.impl.DefaultAnyType;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.FunctionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.extension.api.property.SourceClusterSupportModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationFactory;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.delegate.DefaultExtensionModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.delegate.StereotypeModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ConfigurationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ConnectionProviderModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.DefaultOutputModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.FunctionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OutputModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.SourceModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserFactory;
import org.mule.sdk.api.annotation.Extension;
import org.mule.sdk.api.annotation.source.SourceClusterSupport;

import java.util.List;
import java.util.Optional;

public class MinMuleVersionLoaderDelegatesTestCase {

  private DefaultExtensionLoadingContext createExtensionLoadingContext() {
    ClassLoader classLoader = getClass().getClassLoader();
    DslResolvingContext resolvingContext = getDefault(emptySet());
    DefaultExtensionLoadingContext loadingContext = new DefaultExtensionLoadingContext(classLoader, resolvingContext);
    loadingContext.addParameter(VERSION, getProductVersion());
    loadingContext.addParameter(EXTENSION_TYPE, DummyExtension.class);
    return loadingContext;
  }

  private JavaExtensionModelParserFactory createExtensionParserFactoryForMinMuleVersion(DefaultExtensionLoadingContext loadingContext,
                                                                                        Optional<MuleVersion> muleVersion) {
    DefaultAnyType defaultAnyType = new DefaultAnyType(MetadataFormat.JAVA, ImmutableMap.of());
    DefaultOutputModelParser defaultOutputModelParser = new DefaultOutputModelParser(defaultAnyType, false);

    ConnectionProviderModelParser connectionProviderModelParser = mock(ConnectionProviderModelParser.class);
    when(connectionProviderModelParser.getMinMuleVersion()).thenReturn(muleVersion);
    when(connectionProviderModelParser.getName()).thenReturn(this.getClass().getName());

    FunctionModelParser functionModelParser = mock(FunctionModelParser.class);
    when(functionModelParser.getMinMuleVersion()).thenReturn(muleVersion);
    when(functionModelParser.getName()).thenReturn(this.getClass().getName());
    when(functionModelParser.getOutputType()).thenReturn(mock(OutputModelParser.class));
    when(functionModelParser.getAdditionalModelProperties()).thenReturn(ImmutableList.of());

    OperationModelParser operationModelParser = mock(OperationModelParser.class);
    when(operationModelParser.getMinMuleVersion()).thenReturn(muleVersion);
    when(operationModelParser.getName()).thenReturn(this.getClass().getName());
    when(operationModelParser.getOutputType()).thenReturn(defaultOutputModelParser);
    when(operationModelParser.getAttributesOutputType()).thenReturn(defaultOutputModelParser);

    ParameterModelParser parameterModelParser = mock(ParameterModelParser.class);
    when(parameterModelParser.getMinMuleVersion()).thenReturn(muleVersion);
    when(parameterModelParser.getName()).thenReturn(this.getClass().getName());
    when(parameterModelParser.getRole()).thenReturn(ParameterRole.PRIMARY_CONTENT);
    when(parameterModelParser.getType()).thenReturn(defaultAnyType);
    ParameterGroupModelParser parameterGroupModelParser = mock(ParameterGroupModelParser.class);
    when(parameterGroupModelParser.getParameterParsers()).thenReturn(ImmutableList.of(parameterModelParser));
    when(parameterGroupModelParser.getName()).thenReturn(this.getClass().getName());

    ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);
    ConfigurationFactoryModelProperty configurationFactoryModelProperty =
        new ConfigurationFactoryModelProperty(configurationFactory);
    ConfigurationModelParser configurationModelParser = mock(ConfigurationModelParser.class);
    when(configurationModelParser.getMinMuleVersion()).thenReturn(muleVersion);
    when(configurationModelParser.getName()).thenReturn(this.getClass().getName());
    when(configurationModelParser.getParameterGroupParsers()).thenReturn(ImmutableList.of(parameterGroupModelParser));
    when(configurationModelParser.getConfigurationFactoryModelProperty()).thenReturn(configurationFactoryModelProperty);

    SourceModelParser sourceModelParser = mock(SourceModelParser.class);
    when(sourceModelParser.getMinMuleVersion()).thenReturn(muleVersion);
    when(sourceModelParser.getName()).thenReturn(this.getClass().getName());
    when(sourceModelParser.getOutputType()).thenReturn(defaultOutputModelParser);
    when(sourceModelParser.getAttributesOutputType()).thenReturn(defaultOutputModelParser);
    when(sourceModelParser.getSourceClusterSupportModelProperty())
        .thenReturn(new SourceClusterSupportModelProperty(SourceClusterSupport.NOT_SUPPORTED));

    ExtensionElement extensionElement = JavaExtensionModelParserFactory.getExtensionElement(loadingContext);
    StereotypeModelLoaderDelegate stereotypeModelLoaderDelegate = new StereotypeModelLoaderDelegate(loadingContext);
    ExtensionModelParser parser =
        spy(new JavaExtensionModelParser(extensionElement, stereotypeModelLoaderDelegate, loadingContext));
    when(parser.getConnectionProviderModelParsers()).thenReturn(ImmutableList.of(connectionProviderModelParser));
    when(parser.getFunctionModelParsers()).thenReturn(ImmutableList.of(functionModelParser));
    when(parser.getOperationModelParsers()).thenReturn(ImmutableList.of(operationModelParser));
    when(parser.getConfigurationParsers()).thenReturn(ImmutableList.of(configurationModelParser));
    when(parser.getSourceModelParsers()).thenReturn(ImmutableList.of(sourceModelParser));
    when(parser.getMinMuleVersion()).thenReturn(muleVersion);

    JavaExtensionModelParserFactory parserFactory = mock(JavaExtensionModelParserFactory.class);
    when(parserFactory.createParser(any())).thenReturn(parser);

    return parserFactory;
  }


  private ExtensionDeclaration createExtensionDeclarationForMinMuleVersion(Optional<MuleVersion> muleVersion) {
    DefaultExtensionLoadingContext loadingContext = createExtensionLoadingContext();
    JavaExtensionModelParserFactory parserFactory =
        createExtensionParserFactoryForMinMuleVersion(loadingContext, muleVersion);

    DefaultExtensionModelLoaderDelegate delegate = new DefaultExtensionModelLoaderDelegate(getProductVersion());
    ExtensionDeclarer extensionDeclarer = delegate.declare(parserFactory, loadingContext);
    return extensionDeclarer.getDeclaration();
  }

  private void assertOptionalsAreEqual(Optional<?> p1, Optional<?> p2) {
    assertThat(p1, equalTo(p2));
    assertThat(p1.isPresent(), equalTo(p2.isPresent()));
    if (p1.isPresent() && p2.isPresent()) {
      assertThat(p1.get(), equalTo(p2.get()));
    }
  }

  private void assertExtensionDeclarationForMinMuleVersion(Optional<MuleVersion> minMuleVersion) {
    ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForMinMuleVersion(minMuleVersion);


    assertOptionalsAreEqual(extensionDeclaration.getMinMuleVersion(), minMuleVersion);
  }

  private void assertOperationDeclarationForMinMuleVersion(Optional<MuleVersion> minMuleVersion) {
    ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForMinMuleVersion(minMuleVersion);

    List<OperationDeclaration> operationDeclarationList = extensionDeclaration.getOperations();
    assertThat(operationDeclarationList.size(), equalTo(1));
    OperationDeclaration operationDeclaration = operationDeclarationList.get(0);

    assertOptionalsAreEqual(operationDeclaration.getMinMuleVersion(), minMuleVersion);
  }

  private void assertFunctionDeclarationForMinMuleVersion(Optional<MuleVersion> minMuleVersion) {
    ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForMinMuleVersion(minMuleVersion);

    List<FunctionDeclaration> functionDeclarationList = extensionDeclaration.getFunctions();
    assertThat(functionDeclarationList.size(), equalTo(1));
    FunctionDeclaration functionDeclaration = functionDeclarationList.get(0);

    assertOptionalsAreEqual(functionDeclaration.getMinMuleVersion(), minMuleVersion);

  }

  private void assertConnectionProviderDeclarationForMinMuleVersion(Optional<MuleVersion> minMuleVersion) {
    ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForMinMuleVersion(minMuleVersion);

    List<ConnectionProviderDeclaration> connectionProviderDeclarationList = extensionDeclaration.getConnectionProviders();
    assertThat(connectionProviderDeclarationList.size(), equalTo(1));
    ConnectionProviderDeclaration connectionProviderDeclaration = connectionProviderDeclarationList.get(0);

    assertOptionalsAreEqual(connectionProviderDeclaration.getMinMuleVersion(), minMuleVersion);
  }

  private void assertConfigurationDeclarationForMinMuleVersion(Optional<MuleVersion> minMuleVersion) {
    ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForMinMuleVersion(minMuleVersion);

    List<ConfigurationDeclaration> configurationDeclarationList = extensionDeclaration.getConfigurations();
    assertThat(configurationDeclarationList.size(), equalTo(1));
    ConfigurationDeclaration configurationDeclaration = configurationDeclarationList.get(0);

    assertOptionalsAreEqual(configurationDeclaration.getMinMuleVersion(), minMuleVersion);
  }

  private void assertParameterDeclarationForMinMuleVersion(Optional<MuleVersion> minMuleVersion) {
    ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForMinMuleVersion(minMuleVersion);

    List<ConfigurationDeclaration> configurationDeclarationList = extensionDeclaration.getConfigurations();
    assertThat(configurationDeclarationList.size(), equalTo(1));
    ConfigurationDeclaration configurationDeclaration = configurationDeclarationList.get(0);

    List<ParameterGroupDeclaration> parameterGroupDeclarationList = configurationDeclaration.getParameterGroups();
    assertThat(parameterGroupDeclarationList.size(), equalTo(1));
    ParameterGroupDeclaration parameterGroupDeclaration = parameterGroupDeclarationList.get(0);

    List<ParameterDeclaration> parameterDeclarationList = parameterGroupDeclaration.getParameters();
    assertThat(parameterDeclarationList.size(), equalTo(1));
    ParameterDeclaration parameterDeclaration = parameterDeclarationList.get(0);

    assertOptionalsAreEqual(parameterDeclaration.getMinMuleVersion(), minMuleVersion);
  }

  private void assertSourceDeclarationForMinMuleVersion(Optional<MuleVersion> minMuleVersion) {
    ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForMinMuleVersion(minMuleVersion);

    List<SourceDeclaration> sourceDeclarationList = extensionDeclaration.getMessageSources();
    assertThat(sourceDeclarationList.size(), equalTo(1));
    SourceDeclaration sourceDeclaration = sourceDeclarationList.get(0);

    assertOptionalsAreEqual(sourceDeclaration.getMinMuleVersion(), minMuleVersion);
  }

  @Test
  public void testExtensionDeclarationWithMinMuleVersion() {
    Optional<MuleVersion> minMuleVersion = Optional.of(new MuleVersion("4.5.0"));
    assertExtensionDeclarationForMinMuleVersion(minMuleVersion);
  }

  @Test
  public void testExtensionDeclarationWithoutMinMuleVersion() {
    Optional<MuleVersion> minMuleVersion = Optional.empty();
    assertExtensionDeclarationForMinMuleVersion(minMuleVersion);
  }

  @Test
  public void testConnectionProviderDeclarationWithMinMuleVersion() {
    Optional<MuleVersion> minMuleVersion = Optional.of(new MuleVersion("4.5.0"));
    assertConnectionProviderDeclarationForMinMuleVersion(minMuleVersion);
  }

  @Test
  public void testConnectionProviderDeclarationWithoutMinMuleVersion() {
    Optional<MuleVersion> minMuleVersion = Optional.empty();
    assertConnectionProviderDeclarationForMinMuleVersion(minMuleVersion);
  }

  @Test
  public void testFunctionDeclarationWithMinMuleVersion() {
    Optional<MuleVersion> minMuleVersion = Optional.of(new MuleVersion("4.5.0"));
    assertFunctionDeclarationForMinMuleVersion(minMuleVersion);
  }

  @Test
  public void testFunctionDeclarationWithoutMinMuleVersion() {
    Optional<MuleVersion> minMuleVersion = Optional.empty();
    assertFunctionDeclarationForMinMuleVersion(minMuleVersion);
  }

  @Test
  public void testOperationDeclarationWithSinceMinMuleVersion() {
    Optional<MuleVersion> minMuleVersion = Optional.of(new MuleVersion("4.5.0"));
    assertOperationDeclarationForMinMuleVersion(minMuleVersion);
  }

  @Test
  public void testOperationDeclarationWithoutMinMuleVersion() {
    Optional<MuleVersion> minMuleVersion = Optional.empty();
    assertOperationDeclarationForMinMuleVersion(minMuleVersion);
  }

  @Test
  public void testConfigurationDeclarationWithMinMuleVersion() {
    Optional<MuleVersion> minMuleVersion = Optional.of(new MuleVersion("4.5.0"));
    assertConfigurationDeclarationForMinMuleVersion(minMuleVersion);
  }

  @Test
  public void testConfigurationDeclarationWithoutMinMuleVersion() {
    Optional<MuleVersion> minMuleVersion = Optional.empty();
    assertConfigurationDeclarationForMinMuleVersion(minMuleVersion);
  }

  @Test
  public void testParameterDeclarationWithMinMuleVersion() {
    Optional<MuleVersion> minMuleVersion = Optional.of(new MuleVersion("4.5.0"));
    assertParameterDeclarationForMinMuleVersion(minMuleVersion);
  }

  @Test
  public void testParameterDeclarationWithoutMinMuleVersion() {
    Optional<MuleVersion> minMuleVersion = Optional.empty();
    assertParameterDeclarationForMinMuleVersion(minMuleVersion);
  }

  @Test
  public void testSourceDeclarationWithMinMuleVersion() {
    Optional<MuleVersion> minMuleVersion = Optional.of(new MuleVersion("4.5.0"));
    assertSourceDeclarationForMinMuleVersion(minMuleVersion);
  }

  @Test
  public void testSourceDeclarationWithoutMinMuleVersion() {
    Optional<MuleVersion> minMuleVersion = Optional.empty();
    assertSourceDeclarationForMinMuleVersion(minMuleVersion);
  }

  @Extension(name = DummyExtension.NAME, category = SELECT)
  public static class DummyExtension implements Lifecycle {

    public static final String NAME = "Dummy";

    @Override
    public void dispose() {

    }

    @Override
    public void initialise() throws InitialisationException {

    }

    @Override
    public void start() throws MuleException {

    }

    @Override
    public void stop() throws MuleException {

    }
  }
}
