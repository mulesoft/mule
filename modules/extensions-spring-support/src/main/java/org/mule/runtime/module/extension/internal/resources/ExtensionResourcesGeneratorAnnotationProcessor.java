/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.resources;

import static javax.lang.model.SourceVersion.RELEASE_8;
import static org.mule.runtime.module.extension.internal.resources.BaseExtensionResourcesGeneratorAnnotationProcessor.EXTENSION_VERSION;

import org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.internal.capability.xml.description.DescriptionDeclarationEnricher;
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

  @Override
  protected void configureLoadingRequest(ExtensionModelLoadingRequest.Builder requestBuilder) {
    super.configureLoadingRequest(requestBuilder);

    requestBuilder.addEnricher(new DescriptionDeclarationEnricher());
    requestBuilder.addValidator(new ExportedPackagesValidator());
  }

  @Override
  protected ExtensionModelLoader getExtensionModelLoader() {
    return fetchJavaExtensionModelLoader();
  }
}
