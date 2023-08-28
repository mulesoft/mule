/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.resources.manifest;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;

import java.util.Optional;

/**
 * {@link ClassPackageFinder} implementation that uses the current class loader to obtain the package.
 *
 * @since 4.2.0
 */
public class ClassloaderClassPackageFinder implements ClassPackageFinder {

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<String> packageFor(String className) {
    try {
      Class aClass = loadClass(className, Thread.currentThread().getContextClassLoader());
      return ofNullable(aClass.getPackage().getName());
    } catch (ClassNotFoundException e) {
      return empty();
    }
  }
}
