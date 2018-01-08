/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static java.lang.String.format;
import static javax.lang.model.SourceVersion.RELEASE_8;
import static org.mule.runtime.module.extension.internal.resources.BaseExtensionResourcesGeneratorAnnotationProcessor.EXTENSION_VERSION;

import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.capability.xml.description.DescriptionDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.runtime.module.extension.soap.api.loader.SoapExtensionModelLoader;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.TypeElement;


/**
 * {@link BaseExtensionResourcesGeneratorAnnotationProcessor} implementation for SOAP BASED Extensions that use the
 * soap extensions api classes and annotations.
 *
 * @since 4.0.0
 */
@SupportedAnnotationTypes(value = {"org.mule.runtime.extension.api.annotation.Extension"})
@SupportedSourceVersion(RELEASE_8)
@SupportedOptions(EXTENSION_VERSION)
public class SoapExtensionResourcesGeneratorAnnotationProcessor extends BaseExtensionResourcesGeneratorAnnotationProcessor {

  @Override
  public ExtensionElement toExtensionElement(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
    Class<?> aClass;
    try {
      aClass = Class.forName(processingEnvironment.getElementUtils().getBinaryName(typeElement).toString());
      return new ExtensionTypeWrapper<>(aClass,
                                        new DefaultExtensionsTypeLoaderFactory().createTypeLoader(aClass.getClassLoader()));
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(format("Unable to load class for extension: %s", typeElement), e);
    }
  }

  @Override
  protected ExtensionModelLoader getExtensionModelLoader() {
    return new SoapExtensionModelLoader() {

      @Override
      protected void configureContextBeforeDeclaration(ExtensionLoadingContext context) {
        super.configureContextBeforeDeclaration(context);
        context.addCustomDeclarationEnricher(new DescriptionDeclarationEnricher());
      }
    };
  }


}
