/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

