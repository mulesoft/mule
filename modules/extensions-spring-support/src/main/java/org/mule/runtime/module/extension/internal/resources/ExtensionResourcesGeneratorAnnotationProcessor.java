/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static java.lang.Thread.currentThread;
import static javax.lang.model.SourceVersion.RELEASE_8;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.resources.BaseExtensionResourcesGeneratorAnnotationProcessor.EXTENSION_VERSION;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelLoaderProvider;
import org.mule.runtime.module.extension.internal.capability.xml.description.DescriptionDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.resources.validator.ExportedPackagesValidator;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;


/**
 * {@link BaseExtensionResourcesGeneratorAnnotationProcessor} implementation for default java based extensions.
 *
 * @since 3.7.0
 */
@SupportedAnnotationTypes(value = {"org.mule.runtime.extension.api.annotation.Extension"})
@SupportedSourceVersion(RELEASE_8)
@SupportedOptions(EXTENSION_VERSION)
public class ExtensionResourcesGeneratorAnnotationProcessor extends ClassExtensionResourcesGeneratorAnnotationProcessor {

  private LazyValue<ExtensionModelLoader> extensionModelLoader = new LazyValue<>(this::fetchJavaExtensionModelLoader);

  @Override
  protected ExtensionModelLoader getExtensionModelLoader() {
    return withContextClassLoader(ExtensionModelLoader.class.getClassLoader(), () -> new DefaultJavaExtensionModelLoader() {

      @Override
      protected void configureContextBeforeDeclaration(ExtensionLoadingContext context) {
        super.configureContextBeforeDeclaration(context);
        context.addCustomDeclarationEnricher(new DescriptionDeclarationEnricher());
        context.addCustomValidator(new ExportedPackagesValidator());
      }
    });
  }

  private ExtensionModelLoader lookupExtensionModelLoader() {
    fetchJavaExtensionModelLoader().loadExtensionModel()
  }

  private ExtensionModelLoader fetchJavaExtensionModelLoader() {
    return new SpiServiceRegistry().lookupProviders(ExtensionModelLoaderProvider.class, currentThread().getContextClassLoader())
        .stream()
        .flatMap(p -> p.getExtensionModelLoaders().stream())
        .filter(loader -> "java".equals(loader.getId()))
        .findFirst()
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("java ExtensionModelLoader not found")));
  }
}

}
