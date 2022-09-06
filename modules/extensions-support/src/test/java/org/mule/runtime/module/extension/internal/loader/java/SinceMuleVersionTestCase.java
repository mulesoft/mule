package org.mule.runtime.module.extension.internal.loader.java;

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
import org.mule.runtime.extension.api.property.SinceMuleVersionModelProperty;
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

public class SinceMuleVersionTestCase {

  private DefaultExtensionLoadingContext createExtensionLoadingContext() {
    ClassLoader classLoader = getClass().getClassLoader();
    DslResolvingContext resolvingContext = getDefault(emptySet());
    DefaultExtensionLoadingContext loadingContext = new DefaultExtensionLoadingContext(classLoader, resolvingContext);
    loadingContext.addParameter(VERSION, getProductVersion());
    loadingContext.addParameter(EXTENSION_TYPE, DummyExtension.class);
    return loadingContext;
  }

  private JavaExtensionModelParserFactory createExtensionParserFactoryForSinceMuleVersionModelProperty(DefaultExtensionLoadingContext loadingContext,
                                                                                                       Optional<SinceMuleVersionModelProperty> property) {
    DefaultAnyType defaultAnyType = new DefaultAnyType(MetadataFormat.JAVA, ImmutableMap.of());
    DefaultOutputModelParser defaultOutputModelParser = new DefaultOutputModelParser(defaultAnyType, false);

    ConnectionProviderModelParser connectionProviderModelParser = mock(ConnectionProviderModelParser.class);
    when(connectionProviderModelParser.getSinceMuleVersionModelProperty()).thenReturn(property);
    when(connectionProviderModelParser.getName()).thenReturn(this.getClass().getName());

    FunctionModelParser functionModelParser = mock(FunctionModelParser.class);
    when(functionModelParser.getSinceMuleVersionModelProperty()).thenReturn(property);
    when(functionModelParser.getName()).thenReturn(this.getClass().getName());
    when(functionModelParser.getOutputType()).thenReturn(mock(OutputModelParser.class));
    when(functionModelParser.getAdditionalModelProperties()).thenReturn(ImmutableList.of());

    OperationModelParser operationModelParser = mock(OperationModelParser.class);
    when(operationModelParser.getSinceMuleVersionModelProperty()).thenReturn(property);
    when(operationModelParser.getName()).thenReturn(this.getClass().getName());
    when(operationModelParser.getOutputType()).thenReturn(defaultOutputModelParser);
    when(operationModelParser.getAttributesOutputType()).thenReturn(defaultOutputModelParser);

    ParameterModelParser parameterModelParser = mock(ParameterModelParser.class);
    when(parameterModelParser.getSinceMuleVersionModelProperty()).thenReturn(property);
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
    when(configurationModelParser.getSinceMuleVersionModelProperty()).thenReturn(property);
    when(configurationModelParser.getName()).thenReturn(this.getClass().getName());
    when(configurationModelParser.getParameterGroupParsers()).thenReturn(ImmutableList.of(parameterGroupModelParser));
    when(configurationModelParser.getConfigurationFactoryModelProperty()).thenReturn(configurationFactoryModelProperty);

    SourceModelParser sourceModelParser = mock(SourceModelParser.class);
    when(sourceModelParser.getSinceMuleVersionModelProperty()).thenReturn(property);
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
    when(parser.getSinceMuleVersionModelProperty()).thenReturn(property);

    JavaExtensionModelParserFactory parserFactory = mock(JavaExtensionModelParserFactory.class);
    when(parserFactory.createParser(any())).thenReturn(parser);

    return parserFactory;
  }


  private ExtensionDeclaration createExtensionDeclarationForSinceMuleVersionModelProperty(Optional<SinceMuleVersionModelProperty> property) {
    DefaultExtensionLoadingContext loadingContext = createExtensionLoadingContext();
    JavaExtensionModelParserFactory parserFactory =
        createExtensionParserFactoryForSinceMuleVersionModelProperty(loadingContext, property);

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

  private void assertExtensionDeclarationForSinceMuleVersionModelProperty(Optional<SinceMuleVersionModelProperty> property) {
    ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForSinceMuleVersionModelProperty(property);

    Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty =
        extensionDeclaration.getModelProperty(SinceMuleVersionModelProperty.class);
    assertOptionalsAreEqual(sinceMuleVersionModelProperty, property);
  }

  private void assertOperationDeclarationForSinceMuleVersionModelProperty(Optional<SinceMuleVersionModelProperty> property) {
    ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForSinceMuleVersionModelProperty(property);

    List<OperationDeclaration> operationDeclarationList = extensionDeclaration.getOperations();
    assertThat(operationDeclarationList.size(), equalTo(1));
    OperationDeclaration operationDeclaration = operationDeclarationList.get(0);

    Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty =
        operationDeclaration.getModelProperty(SinceMuleVersionModelProperty.class);
    assertOptionalsAreEqual(sinceMuleVersionModelProperty, property);
  }

  private void assertFunctionDeclarationForSinceMuleVersionModelProperty(Optional<SinceMuleVersionModelProperty> property) {
    ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForSinceMuleVersionModelProperty(property);

    List<FunctionDeclaration> functionDeclarationList = extensionDeclaration.getFunctions();
    assertThat(functionDeclarationList.size(), equalTo(1));
    FunctionDeclaration functionDeclaration = functionDeclarationList.get(0);

    Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty =
        functionDeclaration.getModelProperty(SinceMuleVersionModelProperty.class);
    assertOptionalsAreEqual(sinceMuleVersionModelProperty, property);

  }

  private void assertConnectionProviderDeclarationForSinceMuleVersionModelProperty(Optional<SinceMuleVersionModelProperty> property) {
    ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForSinceMuleVersionModelProperty(property);

    List<ConnectionProviderDeclaration> connectionProviderDeclarationList = extensionDeclaration.getConnectionProviders();
    assertThat(connectionProviderDeclarationList.size(), equalTo(1));
    ConnectionProviderDeclaration connectionProviderDeclaration = connectionProviderDeclarationList.get(0);

    Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty =
        connectionProviderDeclaration.getModelProperty(SinceMuleVersionModelProperty.class);
    assertOptionalsAreEqual(sinceMuleVersionModelProperty, property);
  }

  private void assertConfigurationDeclarationForSinceMuleVersionModelProperty(Optional<SinceMuleVersionModelProperty> property) {
    ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForSinceMuleVersionModelProperty(property);

    List<ConfigurationDeclaration> configurationDeclarationList = extensionDeclaration.getConfigurations();
    assertThat(configurationDeclarationList.size(), equalTo(1));
    ConfigurationDeclaration configurationDeclaration = configurationDeclarationList.get(0);

    Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty =
        configurationDeclaration.getModelProperty(SinceMuleVersionModelProperty.class);
    assertOptionalsAreEqual(sinceMuleVersionModelProperty, property);
  }

  private void assertParameterDeclarationForSinceMuleVersionModelProperty(Optional<SinceMuleVersionModelProperty> property) {
    ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForSinceMuleVersionModelProperty(property);

    List<ConfigurationDeclaration> configurationDeclarationList = extensionDeclaration.getConfigurations();
    assertThat(configurationDeclarationList.size(), equalTo(1));
    ConfigurationDeclaration configurationDeclaration = configurationDeclarationList.get(0);

    List<ParameterGroupDeclaration> parameterGroupDeclarationList = configurationDeclaration.getParameterGroups();
    assertThat(parameterGroupDeclarationList.size(), equalTo(1));
    ParameterGroupDeclaration parameterGroupDeclaration = parameterGroupDeclarationList.get(0);

    List<ParameterDeclaration> parameterDeclarationList = parameterGroupDeclaration.getParameters();
    assertThat(parameterDeclarationList.size(), equalTo(1));
    ParameterDeclaration parameterDeclaration = parameterDeclarationList.get(0);

    Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty =
        parameterDeclaration.getModelProperty(SinceMuleVersionModelProperty.class);
    assertOptionalsAreEqual(sinceMuleVersionModelProperty, property);
  }

  private void assertSourceDeclarationForSinceMuleVersionModelProperty(Optional<SinceMuleVersionModelProperty> property) {
    ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForSinceMuleVersionModelProperty(property);

    List<SourceDeclaration> sourceDeclarationList = extensionDeclaration.getMessageSources();
    assertThat(sourceDeclarationList.size(), equalTo(1));
    SourceDeclaration sourceDeclaration = sourceDeclarationList.get(0);

    Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty =
        sourceDeclaration.getModelProperty(SinceMuleVersionModelProperty.class);
    assertOptionalsAreEqual(sinceMuleVersionModelProperty, property);
  }

  @Test
  public void testExtensionDeclarationWithSinceMuleVersionModelProperty() {
    Optional<SinceMuleVersionModelProperty> property = Optional.of(new SinceMuleVersionModelProperty("4.5.0"));
    assertExtensionDeclarationForSinceMuleVersionModelProperty(property);
  }

  @Test
  public void testExtensionDeclarationWithoutSinceMuleVersionModelProperty() {
    Optional<SinceMuleVersionModelProperty> property = Optional.empty();
    assertExtensionDeclarationForSinceMuleVersionModelProperty(property);
  }

  @Test
  public void testConnectionProviderDeclarationWithSinceMuleVersionModelProperty() {
    Optional<SinceMuleVersionModelProperty> property = Optional.of(new SinceMuleVersionModelProperty("4.5.0"));
    assertConnectionProviderDeclarationForSinceMuleVersionModelProperty(property);
  }

  @Test
  public void testConnectionProviderDeclarationWithoutSinceMuleVersionModelProperty() {
    Optional<SinceMuleVersionModelProperty> property = Optional.empty();
    assertConnectionProviderDeclarationForSinceMuleVersionModelProperty(property);
  }

  @Test
  public void testFunctionDeclarationWithSinceMuleVersionModelProperty() {
    Optional<SinceMuleVersionModelProperty> property = Optional.of(new SinceMuleVersionModelProperty("4.5.0"));
    assertFunctionDeclarationForSinceMuleVersionModelProperty(property);
  }

  @Test
  public void testFunctionDeclarationWithoutSinceMuleVersionModelProperty() {
    Optional<SinceMuleVersionModelProperty> property = Optional.empty();
    assertFunctionDeclarationForSinceMuleVersionModelProperty(property);
  }

  @Test
  public void testOperationDeclarationWithSinceMuleVersionModelProperty() {
    Optional<SinceMuleVersionModelProperty> property = Optional.of(new SinceMuleVersionModelProperty("4.5.0"));
    assertOperationDeclarationForSinceMuleVersionModelProperty(property);
  }

  @Test
  public void testOperationDeclarationWithoutSinceMuleVersionModelProperty() {
    Optional<SinceMuleVersionModelProperty> property = Optional.empty();
    assertOperationDeclarationForSinceMuleVersionModelProperty(property);
  }

  @Test
  public void testConfigurationDeclarationWithSinceMuleVersionModelProperty() {
    Optional<SinceMuleVersionModelProperty> property = Optional.of(new SinceMuleVersionModelProperty("4.5.0"));
    assertConfigurationDeclarationForSinceMuleVersionModelProperty(property);
  }

  @Test
  public void testConfigurationDeclarationWithoutSinceMuleVersionModelProperty() {
    Optional<SinceMuleVersionModelProperty> property = Optional.empty();
    assertConfigurationDeclarationForSinceMuleVersionModelProperty(property);
  }

  @Test
  public void testParameterDeclarationWithSinceMuleVersionModelProperty() {
    Optional<SinceMuleVersionModelProperty> property = Optional.of(new SinceMuleVersionModelProperty("4.5.0"));
    assertParameterDeclarationForSinceMuleVersionModelProperty(property);
  }

  @Test
  public void testParameterDeclarationWithoutSinceMuleVersionModelProperty() {
    Optional<SinceMuleVersionModelProperty> property = Optional.empty();
    assertParameterDeclarationForSinceMuleVersionModelProperty(property);
  }

  @Test
  public void testSourceDeclarationWithSinceMuleVersionModelProperty() {
    Optional<SinceMuleVersionModelProperty> property = Optional.of(new SinceMuleVersionModelProperty("4.5.0"));
    assertSourceDeclarationForSinceMuleVersionModelProperty(property);
  }

  @Test
  public void testSourceDeclarationWithoutSinceMuleVersionModelProperty() {
    Optional<SinceMuleVersionModelProperty> property = Optional.empty();
    assertSourceDeclarationForSinceMuleVersionModelProperty(property);
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
