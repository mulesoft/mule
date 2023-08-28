/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.resources.manifest;

import static java.util.Optional.empty;

import java.util.Optional;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * {@link ClassPackageFinder} implementation that uses a {@link ProcessingEnvironment} to obtain the package.
 *
 * @since 4.2.0
 */
public class ProcessingEnvironmentClassPackageFinder implements ClassPackageFinder {

  private ProcessingEnvironment processingEnvironment;

  public ProcessingEnvironmentClassPackageFinder(ProcessingEnvironment processingEnvironment) {
    this.processingEnvironment = processingEnvironment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<String> packageFor(String className) {
    TypeElement typeElement = processingEnvironment.getElementUtils().getTypeElement(className);
    if (typeElement != null) {
      return Optional.of(processingEnvironment.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString());
    }
    return empty();
  }
}
