/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.resources.manifest;

import static java.util.Optional.empty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link ClassPackageFinder} composite implementation that finds the package of a given class using all the contained
 * {@link ClassPackageFinder}.
 *
 * @since 4.1
 */
public class DefaultClassPackageFinder implements ClassPackageFinder {

  private List<ClassPackageFinder> classFinders = new ArrayList<>();

  public DefaultClassPackageFinder() {
    this.classFinders.add(new ClassloaderClassPackageFinder());
  }

  /**
   * Add an additional {@link ClassPackageFinder} to this composite {@link ClassPackageFinder}.
   *
   * @param classPackageFinder The {@link ClassPackageFinder} to be added.
   */
  public void addAdditionalPackageFinder(ClassPackageFinder classPackageFinder) {
    this.classFinders.add(classPackageFinder);
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

}
