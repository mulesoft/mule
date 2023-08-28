/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.launcher.coreextension;

import org.mule.runtime.container.api.MuleCoreExtension;

import java.lang.reflect.Method;

/**
 * Maps a core extension dependency class to a method in a dependant class
 */
public class LinkedMuleCoreExtensionDependency {

  private final Class<? extends MuleCoreExtension> dependencyClass;

  private final Method dependantMethod;

  public LinkedMuleCoreExtensionDependency(Class<? extends MuleCoreExtension> dependencyClass, Method dependantMethod) {
    this.dependencyClass = dependencyClass;
    this.dependantMethod = dependantMethod;
  }

  public Method getDependantMethod() {
    return dependantMethod;
  }

  public Class<? extends MuleCoreExtension> getDependencyClass() {
    return dependencyClass;
  }
}
