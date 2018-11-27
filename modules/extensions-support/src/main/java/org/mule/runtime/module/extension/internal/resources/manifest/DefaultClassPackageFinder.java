/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.manifest;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Default {@link ClassPackageFinder} implementation.
 * <p>
 * By default it uses the current {@link ClassLoader} to look for the {@link Class} package. If a
 * {@link ProcessingEnvironment} is provided it will be also used.
 *
 * @since 4.1
 */
public class DefaultClassPackageFinder implements ClassPackageFinder {

  private List<ClassPackageFinder> classFinders = new ArrayList<>();

  public DefaultClassPackageFinder() {
    classFinders.add(this::usingClassloader);
  }

  public DefaultClassPackageFinder(ProcessingEnvironment processingEnvironment) {
    this();
    if (processingEnvironment != null) {
      classFinders.add(className -> usingProcessingEnvironment(className, processingEnvironment));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<String> packageFor(String className) {
    return classFinders.stream()
        .map(finder -> finder.packageFor(className))
        .filter(Optional::isPresent)
        .findFirst()
        .orElse(empty());
  }

  private Optional<String> usingProcessingEnvironment(String className, ProcessingEnvironment processingEnvironment) {
    if (processingEnvironment != null) {
      TypeElement typeElement = processingEnvironment.getElementUtils().getTypeElement(className);
      if (typeElement != null) {
        return Optional.of(processingEnvironment.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString());
      }
    }
    return empty();
  }

  private Optional<String> usingClassloader(String className) {
    try {
      Class aClass = loadClass(className, Thread.currentThread().getContextClassLoader());
      return ofNullable(aClass.getPackage().getName());
    } catch (ClassNotFoundException e) {
      return empty();
    }
  }
}
