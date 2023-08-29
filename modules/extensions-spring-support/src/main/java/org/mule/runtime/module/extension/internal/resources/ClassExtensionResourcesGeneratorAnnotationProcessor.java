/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static java.lang.String.format;

import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * Abstract Annotation Processor for APTs that works with Java Classes.
 *
 * @since 4.1
 */
public abstract class ClassExtensionResourcesGeneratorAnnotationProcessor
    extends BaseExtensionResourcesGeneratorAnnotationProcessor {

  @Override
  public ExtensionElement toExtensionElement(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
    Class<?> extensionClass = processor.classFor(typeElement, processingEnvironment)
        .orElseThrow(() -> new RuntimeException(format("Unable to load class for extension: %s",
                                                       typeElement.getQualifiedName().toString())));
    return new ExtensionTypeWrapper<>(extensionClass,
                                      new DefaultExtensionsTypeLoaderFactory().createTypeLoader(extensionClass.getClassLoader()));
  }

  @Override
  protected boolean shouldProcess(TypeElement extensionElement, ProcessingEnvironment processingEnv) {
    return processor.classFor(extensionElement, processingEnv).isPresent();
  }
}
