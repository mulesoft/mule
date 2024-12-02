/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.resources;

import static org.mule.runtime.module.extension.api.resources.BaseExtensionResourcesGeneratorAnnotationProcessor.EXTENSION_CLASSES;
import static org.mule.runtime.module.extension.api.resources.BaseExtensionResourcesGeneratorAnnotationProcessor.EXTENSION_RESOURCES;
import static org.mule.runtime.module.extension.api.resources.BaseExtensionResourcesGeneratorAnnotationProcessor.EXTENSION_VERSION;
import static org.mule.runtime.module.extension.internal.resources.validator.ExportedPackagesValidator.EXPORTED_PACKAGES_VALIDATOR_SKIP;
import static org.mule.runtime.module.extension.internal.resources.validator.ExportedPackagesValidator.EXPORTED_PACKAGES_VALIDATOR_STRICT_VALIDATION;

import static javax.lang.model.SourceVersion.RELEASE_17;

import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest;
import org.mule.runtime.module.extension.internal.capability.xml.description.DescriptionDeclarationEnricher;
import org.mule.runtime.module.extension.internal.resources.validator.ExportedPackagesValidator;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;


/**
 * {@link BaseExtensionResourcesGeneratorAnnotationProcessor} implementation for default java based extensions.
 *
 * @since 4.6.0
 */
@SupportedAnnotationTypes({
    "org.mule.runtime.extension.api.annotation.Extension",
    "org.mule.sdk.api.annotation.Extension"
})
@SupportedSourceVersion(RELEASE_17)
@SupportedOptions({
    EXTENSION_VERSION,
    EXTENSION_RESOURCES,
    EXTENSION_CLASSES,
    EXPORTED_PACKAGES_VALIDATOR_SKIP,
    EXPORTED_PACKAGES_VALIDATOR_STRICT_VALIDATION
})
public class ExtensionResourcesGeneratorAnnotationProcessor extends ClassExtensionResourcesGeneratorAnnotationProcessor {

  @Override
  protected void configureLoadingRequest(ExtensionModelLoadingRequest.Builder requestBuilder) {
    super.configureLoadingRequest(requestBuilder);

    requestBuilder.addEnricher(new DescriptionDeclarationEnricher());
    requestBuilder.addValidator(new ExportedPackagesValidator(processingEnv));
  }

  @Override
  protected ExtensionModelLoader getExtensionModelLoader() {
    return fetchJavaExtensionModelLoader();
  }
}
