/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.internal.classloader;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LOG_VERBOSE_CLASSLOADING;

import static java.lang.Boolean.valueOf;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyEnumeration;
import static java.util.Collections.synchronizedSet;
import static java.util.Objects.requireNonNull;

import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.FineGrainedControlClassLoader;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LookupStrategyFilteredClassLoader extends ClassLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(FineGrainedControlClassLoader.class);

  static {
    registerAsParallelCapable();
  }

  private final ClassLoaderLookupPolicy lookupPolicy;
  private final boolean verboseLogging;

  public LookupStrategyFilteredClassLoader(ClassLoader parent, ClassLoaderLookupPolicy lookupPolicy) {
    super(parent);
    requireNonNull(lookupPolicy, "Lookup policy cannot be null");
    this.lookupPolicy = lookupPolicy;
    verboseLogging = valueOf(getProperty(MULE_LOG_VERBOSE_CLASSLOADING));
  }

  private boolean isVerboseLogging() {
    return verboseLogging || LOGGER.isDebugEnabled();
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> result = null;

    final LookupStrategy lookupStrategy = lookupPolicy.getClassLookupStrategy(name);
    if (lookupStrategy == null) {
      throw new NullPointerException(format("Unable to find a lookup strategy for '%s' from %s", name, this));
    }

    if (isVerboseLogging()) {
      logLoadingClass(name, lookupStrategy, "Loading class '%s' with '%s' on '%s'");
    }

    // Gather information about the exceptions in each of the searched class loaders to provide
    // troubleshooting information in case of throwing a ClassNotFoundException.

    List<ClassNotFoundException> exceptions = new ArrayList<>();
    for (ClassLoader classLoader : lookupStrategy.getClassLoaders(this)) {
      try {
        // skip so the child classloader loads this, and avoid overriding the resolution form the parent.
        if (classLoader != this) {
          result = findParentClass(name, classLoader);
        }
      } catch (ClassNotFoundException e) {
        exceptions.add(e);
      }
    }

    // if (result != null) {
    // throw new ClassNotFoundException(name);
    // }

    if (isVerboseLogging()) {
      logLoadedClass(name, result);
    }

    if (resolve) {
      resolveClass(result);
    }

    return result;
  }

  private final Set<Thread> resourceLoadingThreads = synchronizedSet(new HashSet<>());

  @Override
  public URL getResource(String name) {
    if (resourceLoadingThreads.add(currentThread())) {
      try {
        URL url = findResource(name);
        if (url == null && getParent() != null) {
          url = getParent().getResource(name);
        }
        return url;
      } finally {
        resourceLoadingThreads.remove(currentThread());
      }
    } else {
      return null;
    }
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    if (resourceLoadingThreads.add(currentThread())) {
      try {
        Enumeration<URL>[] tmp = (Enumeration<URL>[]) new Enumeration<?>[2];
        tmp[0] = findResources(name);
        if (getParent() != null) {
          tmp[1] = getParent().getResources(name);
        }

        return new org.mule.runtime.core.api.util.CompoundEnumeration<>(tmp);
      } finally {
        resourceLoadingThreads.remove(currentThread());
      }
    } else {
      return emptyEnumeration();
    }
  }

  private void logLoadingClass(String name, LookupStrategy lookupStrategy, String format) {
    final String message = format(format, name, lookupStrategy, this);
    doVerboseLogging(message);
  }

  private void logLoadedClass(String name, Class<?> result) {
    final boolean loadedFromChild = result.getClassLoader() == this;
    final String message = format("Loaded class '%s' from %s: %s", name, (loadedFromChild ? "child" : "parent"),
                                  (loadedFromChild ? this : getParent()));
    doVerboseLogging(message);
  }

  private void doVerboseLogging(String message) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(message);
    } else {
      LOGGER.info(message);
    }
  }

  protected Class<?> findParentClass(String name, ClassLoader classLoader) throws ClassNotFoundException {
    if (classLoader != null) {
      return classLoader.loadClass(name);
    } else {
      return findSystemClass(name);
    }
  }
}
