/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher;

import org.mule.runtime.module.artifact.classloader.ClassLoaderFilter;

/**
 * Provides a {@link ClassLoaderFilter} that does not filter any class or resource.
 */
public class PassThroughClassLoaderFilter implements ClassLoaderFilter {

  @Override
  public boolean exportsClass(String name) {
    return true;
  }

  @Override
  public boolean exportsResource(String name) {
    return true;
  }
}
