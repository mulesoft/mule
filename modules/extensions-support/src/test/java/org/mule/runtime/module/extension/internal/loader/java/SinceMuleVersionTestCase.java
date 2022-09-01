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
import jdk.nashorn.internal.ir.annotations.Immutable;
import org.junit.Test;
import org.mule.metadata.api.model.impl.DefaultAnyType;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.FunctionDeclaration;
import org.mule.runtime.extension.api.property.SinceMuleVersionModelProperty;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionElement;
import org.mule.runtime.module.extension.internal.loader.delegate.DefaultExtensionModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.delegate.StereotypeModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.FunctionWrapper;
import org.mule.runtime.module.extension.internal.loader.parser.ConnectionProviderModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.DefaultOutputModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.FunctionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OutputModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaConnectionProviderModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserFactory;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaFunctionModelParser;
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
		ExtensionElement extensionElement = JavaExtensionModelParserFactory.getExtensionElement(loadingContext);
		StereotypeModelLoaderDelegate stereotypeModelLoaderDelegate = new StereotypeModelLoaderDelegate(loadingContext);

		ExtensionModelParser parser = spy(new JavaExtensionModelParser(extensionElement, stereotypeModelLoaderDelegate, loadingContext));
		when(parser.getSinceMuleVersionModelProperty()).thenReturn(property);

		ConnectionProviderModelParser connectionProviderModelParser = mock(ConnectionProviderModelParser.class);
		when(parser.getConnectionProviderModelParsers()).thenReturn(ImmutableList.of(connectionProviderModelParser));
		when(connectionProviderModelParser.getSinceMuleVersionModelProperty()).thenReturn(property);
		when(connectionProviderModelParser.getName()).thenReturn(this.getClass().getName());

		FunctionModelParser functionModelParser = mock(FunctionModelParser.class);
		when(parser.getFunctionModelParsers()).thenReturn(ImmutableList.of(functionModelParser));
		when(functionModelParser.getSinceMuleVersionModelProperty()).thenReturn(property);
		when(functionModelParser.getOutputType()).thenReturn(mock(OutputModelParser.class));
		when(functionModelParser.getAdditionalModelProperties()).thenReturn(ImmutableList.of());

		JavaExtensionModelParserFactory parserFactory = mock(JavaExtensionModelParserFactory.class);
		when(parserFactory.createParser(any())).thenReturn(parser);

		return parserFactory;
	}

	private void assertSinceMuleVersionModelPropertiesEqual(Optional<SinceMuleVersionModelProperty> p1, Optional<SinceMuleVersionModelProperty> p2) {
		assertThat(p1.isPresent()).isEqualTo(p2.isPresent());
		if (p1.isPresent() && p2.isPresent()) {
			assertThat(p1.get()).isEqualTo(p2.get());
		}
	}

	private void assertExtensionDeclarationForSinceMuleVersionModelProperty(Optional<SinceMuleVersionModelProperty> property) {
		DefaultExtensionLoadingContext loadingContext = createExtensionLoadingContext();
		JavaExtensionModelParserFactory parserFactory = createExtensionParserFactoryForSinceMuleVersionModelProperty(loadingContext, property);

		DefaultExtensionModelLoaderDelegate delegate = new DefaultExtensionModelLoaderDelegate(getProductVersion());
		ExtensionDeclarer extensionDeclarer = delegate.declare(parserFactory, loadingContext);
		ExtensionDeclaration extensionDeclaration = extensionDeclarer.getDeclaration();

		Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty = extensionDeclaration.getModelProperty(SinceMuleVersionModelProperty.class);
		assertSinceMuleVersionModelPropertiesEqual(sinceMuleVersionModelProperty, property);

		List<ConnectionProviderDeclaration> connectionProviderDeclarationList = extensionDeclaration.getConnectionProviders();
		assertThat(connectionProviderDeclarationList.size()).isEqualTo(1);
		ConnectionProviderDeclaration connectionProviderDeclaration = connectionProviderDeclarationList.get(0);
		sinceMuleVersionModelProperty = connectionProviderDeclaration.getModelProperty(SinceMuleVersionModelProperty.class);
		assertSinceMuleVersionModelPropertiesEqual(sinceMuleVersionModelProperty, property);

		List<FunctionDeclaration> functionDeclarationList = extensionDeclaration.getFunctions();
		assertThat(connectionProviderDeclarationList.size()).isEqualTo(1);
		FunctionDeclaration functionDeclaration = functionDeclarationList.get(0);
		sinceMuleVersionModelProperty = functionDeclaration.getModelProperty(SinceMuleVersionModelProperty.class);
		assertSinceMuleVersionModelPropertiesEqual(sinceMuleVersionModelProperty, property);
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
