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

import org.junit.Test;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.property.SinceMuleVersionModelProperty;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.delegate.DefaultExtensionModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserFactory;
import org.mule.sdk.api.annotation.Extension;

import java.util.Optional;

public class SinceMuleVersionTestCase {

	private DefaultExtensionLoadingContext createLoadingContext() {
		ClassLoader classLoader = getClass().getClassLoader();
		DslResolvingContext resolvingContext = getDefault(emptySet());
		DefaultExtensionLoadingContext loadingContext = new DefaultExtensionLoadingContext(classLoader, resolvingContext);
		loadingContext.addParameter(VERSION, getProductVersion());
		loadingContext.addParameter(EXTENSION_TYPE, DummyExtension.class);
		return loadingContext;
	}

	private JavaExtensionModelParserFactory createParserFactory(DefaultExtensionLoadingContext loadingContext, Optional<SinceMuleVersionModelProperty> property) {
		ExtensionModelParser parser = spy(new JavaExtensionModelParser(JavaExtensionModelParserFactory.getExtensionElement(loadingContext), loadingContext));
		when(parser.getSinceMuleVersionModelProperty()).thenReturn(property);

		JavaExtensionModelParserFactory parserFactory = mock(JavaExtensionModelParserFactory.class);
		when(parserFactory.createParser(any())).thenReturn(parser);

		return parserFactory;
	}

	private void assertDeclarationForSinceMuleVersionModelProperty(Optional<SinceMuleVersionModelProperty> property) {
		DefaultExtensionLoadingContext loadingContext = createLoadingContext();
		JavaExtensionModelParserFactory parserFactory = createParserFactory(loadingContext, property);

		DefaultExtensionModelLoaderDelegate delegate = new DefaultExtensionModelLoaderDelegate(getProductVersion());
		ExtensionDeclarer extensionDeclarer = delegate.declare(parserFactory, loadingContext);
		ExtensionDeclaration extensionDeclaration = extensionDeclarer.getDeclaration();

		Optional<SinceMuleVersionModelProperty> sinceMuleVersionModelProperty = extensionDeclaration.getModelProperty(SinceMuleVersionModelProperty.class);
		assertThat(sinceMuleVersionModelProperty.isPresent()).isEqualTo(property.isPresent());
		if (sinceMuleVersionModelProperty.isPresent() && property.isPresent()) {
			assertThat(sinceMuleVersionModelProperty.get()).isEqualTo(property.get());
		}
	}

	@Test
	public void testDeclarationWithSinceMuleVersionModelProperty() {
		Optional<SinceMuleVersionModelProperty> property = Optional.of(new SinceMuleVersionModelProperty("4.5.0"));
		assertDeclarationForSinceMuleVersionModelProperty(property);
	}

	@Test
	public void testDeclarationWithoutSinceMuleVersionModelProperty() {
		Optional<SinceMuleVersionModelProperty> property = Optional.empty();
		assertDeclarationForSinceMuleVersionModelProperty(property);
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
