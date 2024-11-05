/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import static java.beans.Introspector.flushCaches;

import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;

import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ResourceReleaser implementation that references to the artifact class loader to clean up any reference before the class loader
 * is disposed in order to prevent leaks.
 */
public class ClassLoaderResourceReleaser implements ResourceReleaser {

  private final transient Logger logger = LoggerFactory.getLogger(getClass());

  private final ClassLoader classLoader;

  public ClassLoaderResourceReleaser(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public void release() {
    cleanUpResourceBundle();

    clearClassLoaderSoftkeys();
  }

  private void cleanUpResourceBundle() {
    try {
      ResourceBundle.clearCache(this.classLoader);
    } catch (Exception e) {
      logger.warn("Couldn't clean up ResourceBundle. This can cause a memory leak.", e);
    }
  }

  private void clearClassLoaderSoftkeys() {
    try {
      flushCaches();
    } catch (Exception e) {
      logger.warn("Couldn't clear soft keys in caches. This can cause a classloader memory leak.", e);
    }
  }
}
