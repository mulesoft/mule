/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

  private final DefaultMuleClassPathConfig classPath;

  public MuleContainerSystemClassLoader(DefaultMuleClassPathConfig classPath) {
    super(new URL[0], createOptClassloader(classPath.getOptURLs()));

    for (URL url : classPath.getMuleURLs()) {
      addURL(url);
    }

    this.classPath = classPath;
  }

  private static ClassLoader createOptClassloader(List<URL> optUrls) {
    return new URLClassLoader(optUrls.toArray(new URL[optUrls.size()]));
  }

  @Override
  public String toString() {
    return "MuleContainerSystemClassLoader{" + classPath + "}";
  }
}
