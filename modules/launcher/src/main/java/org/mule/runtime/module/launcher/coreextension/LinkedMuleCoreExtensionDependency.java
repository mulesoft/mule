/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
