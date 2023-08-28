/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.resources;

import static javax.lang.model.SourceVersion.RELEASE_8;

import static org.mule.runtime.module.artifact.activation.api.extension.discovery.boot.ExtensionLoaderUtils.getLoaderById;
import static org.mule.runtime.module.extension.internal.resources.BaseExtensionResourcesGeneratorAnnotationProcessor.EXTENSION_VERSION;

import org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.internal.capability.xml.description.DescriptionDeclarationEnricher;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;


/**
 * {@link BaseExtensionResourcesGeneratorAnnotationProcessor} implementation for SOAP BASED Extensions that use the soap
 * extensions api classes and annotations.
 *
 * @since 4.0.0
 */
@SupportedAnnotationTypes(value = {"org.mule.runtime.extension.api.annotation.Extension"})
@SupportedSourceVersion(RELEASE_8)
@SupportedOptions(EXTENSION_VERSION)
public class SoapExtensionResourcesGeneratorAnnotationProcessor extends ClassExtensionResourcesGeneratorAnnotationProcessor {

  @Override
  protected void configureLoadingRequest(ExtensionModelLoadingRequest.Builder requestBuilder) {
    super.configureLoadingRequest(requestBuilder);
    requestBuilder.addEnricher(new DescriptionDeclarationEnricher());
  }

  @Override
  protected ExtensionModelLoader getExtensionModelLoader() {
    return getLoaderById("soap");
  }
}
