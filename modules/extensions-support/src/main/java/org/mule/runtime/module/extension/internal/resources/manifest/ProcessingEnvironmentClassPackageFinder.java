/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
