/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.reboot.internal;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * Loads a standard $MULE_HOME/lib/* hierarchy.
 */
public class MuleContainerSystemClassLoader extends URLClassLoader {

  static {
    registerAsParallelCapable();
  }

  public MuleContainerSystemClassLoader(DefaultMuleClassPathConfig classPath) {
    super(new URL[0], createOptClassloader(classPath.getOptURLs()));

    for (URL url : classPath.getMuleURLs()) {
      addURL(url);
    }
  }

  private static ClassLoader createOptClassloader(List<URL> optUrls) {
    return new URLClassLoader(optUrls.toArray(new URL[optUrls.size()]));
  }
}
