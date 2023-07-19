/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.classloader;

import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;

public class TestResourceReleaser implements ResourceReleaser {

  private ClassLoader classLoader;

  @Override
  public void release() {
    classLoader = this.getClass().getClassLoader();
  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }

}

