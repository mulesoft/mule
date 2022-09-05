package org.mule.runtime.module.extension.internal.loader.java;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
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
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.extension.api.property.SinceMuleVersionModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationFactory;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.delegate.DefaultExtensionModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.delegate.ParameterModelsLoaderDelegate;
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
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserFactory;
import org.mule.sdk.api.annotation.Configuration;
import org.mule.sdk.api.annotation.Extension;

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

	private JavaExtensionModelParserFactory createExtensionParserFactoryForSinceMuleVersionModelProperty(DefaultExtensionLoadingContext loadingContext, Optional<SinceMuleVersionModelProperty> property) {
		DefaultAnyType anyType = new DefaultAnyType(MetadataFormat.JAVA, ImmutableMap.of());

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
		DefaultOutputModelParser defaultOutputModelParser = new DefaultOutputModelParser(anyType, false);
		when(operationModelParser.getOutputType()).thenReturn(defaultOutputModelParser);
		when(operationModelParser.getAttributesOutputType()).thenReturn(defaultOutputModelParser);

		ParameterModelParser parameterModelParser = mock(ParameterModelParser.class);
		when(parameterModelParser.getSinceMuleVersionModelProperty()).thenReturn(property);
		when(parameterModelParser.getName()).thenReturn(this.getClass().getName());
		when(parameterModelParser.getRole()).thenReturn(ParameterRole.PRIMARY_CONTENT);
		when(parameterModelParser.getType()).thenReturn(anyType);
		ParameterGroupModelParser parameterGroupModelParser = mock(ParameterGroupModelParser.class);
		when(parameterGroupModelParser.getParameterParsers()).thenReturn(ImmutableList.of(parameterModelParser));
		when(parameterGroupModelParser.getName()).thenReturn(this.getClass().getName());

		ConfigurationFactory configurationFactory = mock(ConfigurationFactory.class);
		ConfigurationFactoryModelProperty configurationFactoryModelProperty = new ConfigurationFactoryModelProperty(configurationFactory);
		ConfigurationModelParser configurationModelParser = mock(ConfigurationModelParser.class);
		when(configurationModelParser.getSinceMuleVersionModelProperty()).thenReturn(property);
		when(configurationModelParser.getName()).thenReturn(this.getClass().getName());
		when(configurationModelParser.getParameterGroupParsers()).thenReturn(ImmutableList.of(parameterGroupModelParser));
		when(configurationModelParser.getConfigurationFactoryModelProperty()).thenReturn(configurationFactoryModelProperty);

		ExtensionElement extensionElement = JavaExtensionModelParserFactory.getExtensionElement(loadingContext);
		StereotypeModelLoaderDelegate stereotypeModelLoaderDelegate = new StereotypeModelLoaderDelegate(loadingContext);
		ExtensionModelParser parser = spy(new JavaExtensionModelParser(extensionElement, stereotypeModelLoaderDelegate, loadingContext));
		when(parser.getConnectionProviderModelParsers()).thenReturn(ImmutableList.of(connectionProviderModelParser));
		when(parser.getFunctionModelParsers()).thenReturn(ImmutableList.of(functionModelParser));
		when(parser.getOperationModelParsers()).thenReturn(ImmutableList.of(operationModelParser));
		when(parser.getConfigurationParsers()).thenReturn(ImmutableList.of(configurationModelParser));
		when(parser.getSinceMuleVersionModelProperty()).thenReturn(property);

		JavaExtensionModelParserFactory parserFactory = mock(JavaExtensionModelParserFactory.class);
		when(parserFactory.createParser(any())).thenReturn(parser);

		return parserFactory;
	}


	private ExtensionDeclaration createExtensionDeclarationForSinceMuleVersionModelProperty(Optional<SinceMuleVersionModelProperty> property) {
		DefaultExtensionLoadingContext loadingContext = createExtensionLoadingContext();
		JavaExtensionModelParserFactory parserFactory = createExtensionParserFactoryForSinceMuleVersionModelProperty(loadingContext, property);

		DefaultExtensionModelLoaderDelegate delegate = new DefaultExtensionModelLoaderDelegate(getProductVersion());
		ExtensionDeclarer extensionDeclarer = delegate.declare(parserFactory, loadingContext);
		return extensionDeclarer.getDeclaration();
	}

	private void assertOptionalsAreEqual(Optional<?> p1, Optional<?> p2) {
		assertThat(p1.isPresent()).isEqualTo(p2.isPresent());
		if (p1.isPresent() && p2.isPresent()) {
			assertThat(p1.get()).isEqualTo(p2.get());
		}
	}

	private void assertExtensionDeclarationForSinceMuleVersionModelProperty(Optional<SinceMuleVersionModelProperty> property) {
		ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForSinceMuleVersionModelProperty(property);

		Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty = extensionDeclaration.getModelProperty(SinceMuleVersionModelProperty.class);
		assertOptionalsAreEqual(sinceMuleVersionModelProperty, property);
	}

	private void assertOperationDeclarationForSinceMuleVersionModelProperty(Optional<SinceMuleVersionModelProperty> property) {
		ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForSinceMuleVersionModelProperty(property);

		List<OperationDeclaration> operationDeclarationList = extensionDeclaration.getOperations();
		assertThat(operationDeclarationList.size()).isEqualTo(1);
		OperationDeclaration operationDeclaration = operationDeclarationList.get(0);

		Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty = operationDeclaration.getModelProperty(SinceMuleVersionModelProperty.class);
		assertOptionalsAreEqual(sinceMuleVersionModelProperty, property);
	}

	private void assertFunctionDeclarationForSinceMuleVersionModelProperty(Optional<SinceMuleVersionModelProperty> property) {
		ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForSinceMuleVersionModelProperty(property);

		List<FunctionDeclaration> functionDeclarationList = extensionDeclaration.getFunctions();
		assertThat(functionDeclarationList.size()).isEqualTo(1);
		FunctionDeclaration functionDeclaration = functionDeclarationList.get(0);

		Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty = functionDeclaration.getModelProperty(SinceMuleVersionModelProperty.class);
		assertOptionalsAreEqual(sinceMuleVersionModelProperty, property);

	}

	private void assertConnectionProviderDeclarationForSinceMuleVersionModelProperty(Optional<SinceMuleVersionModelProperty> property) {
		ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForSinceMuleVersionModelProperty(property);

		List<ConnectionProviderDeclaration> connectionProviderDeclarationList = extensionDeclaration.getConnectionProviders();
		assertThat(connectionProviderDeclarationList.size()).isEqualTo(1);
		ConnectionProviderDeclaration connectionProviderDeclaration = connectionProviderDeclarationList.get(0);

		Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty = connectionProviderDeclaration.getModelProperty(SinceMuleVersionModelProperty.class);
		assertOptionalsAreEqual(sinceMuleVersionModelProperty, property);
	}

	private void assertConfigurationDeclarationForSinceMuleVersionModelProperty(Optional<SinceMuleVersionModelProperty> property) {
		ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForSinceMuleVersionModelProperty(property);

		List<ConfigurationDeclaration> configurationDeclarationList = extensionDeclaration.getConfigurations();
		assertThat(configurationDeclarationList.size()).isEqualTo(1);
		ConfigurationDeclaration configurationDeclaration = configurationDeclarationList.get(0);

		Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty = configurationDeclaration.getModelProperty(SinceMuleVersionModelProperty.class);
		assertOptionalsAreEqual(sinceMuleVersionModelProperty, property);
	}

	private void assertParameterDeclarationForSinceMuleVersionModelProperty(Optional<SinceMuleVersionModelProperty> property) {
		ExtensionDeclaration extensionDeclaration = createExtensionDeclarationForSinceMuleVersionModelProperty(property);

		List<ConfigurationDeclaration> configurationDeclarationList = extensionDeclaration.getConfigurations();
		assertThat(configurationDeclarationList.size()).isEqualTo(1);
		ConfigurationDeclaration configurationDeclaration = configurationDeclarationList.get(0);

		List<ParameterGroupDeclaration> parameterGroupDeclarationList = configurationDeclaration.getParameterGroups();
		assertThat(parameterGroupDeclarationList.size()).isEqualTo(1);
		ParameterGroupDeclaration parameterGroupDeclaration = parameterGroupDeclarationList.get(0);

		List<ParameterDeclaration> parameterDeclarationList = parameterGroupDeclaration.getParameters();
		assertThat(parameterDeclarationList.size()).isEqualTo(1);
		ParameterDeclaration parameterDeclaration = parameterDeclarationList.get(0);

		Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty = parameterDeclaration.getModelProperty(SinceMuleVersionModelProperty.class);
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

	@Extension(name = DummyExtension.NAME, category = SELECT)
	public static class DummyExtension implements Lifecycle {

		public static final String NAME = "Dummy";

		@Override public void dispose() {

		}

		@Override public void initialise() throws InitialisationException {

		}

		@Override public void start() throws MuleException {

		}

		@Override public void stop() throws MuleException {

		}
	}
}
