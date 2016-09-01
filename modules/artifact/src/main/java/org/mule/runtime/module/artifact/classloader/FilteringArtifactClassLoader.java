/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.classloader;

import static java.lang.Boolean.valueOf;
import static java.lang.Integer.toHexString;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.System.identityHashCode;
import static java.util.Collections.emptyList;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_LOG_VERBOSE_CLASSLOADING;
import static org.mule.runtime.core.util.Preconditions.checkArgument;

import org.mule.runtime.module.artifact.classloader.exception.NotExportedClassException;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a {@link ClassLoader} that filter which classes and resources can be resolved based on a {@link ClassLoaderFilter}
 */
public class FilteringArtifactClassLoader extends ClassLoader implements ArtifactClassLoader {

  static {
    registerAsParallelCapable();
  }

  protected static final Logger logger = LoggerFactory.getLogger(FilteringArtifactClassLoader.class);

  private final ArtifactClassLoader artifactClassLoader;
  private final ClassLoaderFilter filter;

  /**
   * Creates a new filtering classLoader
   *
   * @param artifactClassLoader artifact classLoader to filter. Non null
   * @param filter filters access to classes and resources from the artifact classLoader. Non null
   */
  public FilteringArtifactClassLoader(ArtifactClassLoader artifactClassLoader, ClassLoaderFilter filter) {
    checkArgument(artifactClassLoader != null, "ArtifactClassLoader cannot be null");
    checkArgument(filter != null, "Filter cannot be null");

    this.artifactClassLoader = artifactClassLoader;
    this.filter = filter;
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    if (filter.exportsClass(name)) {
      return artifactClassLoader.getClassLoader().loadClass(name);
    } else {
      throw new NotExportedClassException(name, getArtifactName(), filter);
    }
  }

  @Override
  public URL getResource(String name) {
    if (filter.exportsResource(name)) {
      return getResourceFromDelegate(artifactClassLoader, name);
    } else {
      logClassloadingTrace(format("Resource '%s' not found in classloader for '%s'.", name, getArtifactName()));
      logClassloadingTrace(format("Filter applied for resource '%s': %s", name, getArtifactName()));
      return null;
    }
  }

  protected URL getResourceFromDelegate(ArtifactClassLoader artifactClassLoader, String name) {
    return artifactClassLoader.findResource(name);
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    if (filter.exportsResource(name)) {
      return getResourcesFromDelegate(artifactClassLoader, name);
    } else {
      logClassloadingTrace(format("Resources '%s' not found in classloader for '%s'.", name, getArtifactName()));
      logClassloadingTrace(format("Filter applied for resources '%s': %s", name, getArtifactName()));
      return new EnumerationAdapter<>(emptyList());
    }
  }

  private void logClassloadingTrace(String message) {
    if (isVerboseClassLoading()) {
      logger.info(message);
    } else if (logger.isTraceEnabled()) {
      logger.trace(message);
    }
  }

  private Boolean isVerboseClassLoading() {
    return valueOf(getProperty(MULE_LOG_VERBOSE_CLASSLOADING));
  }

  protected Enumeration<URL> getResourcesFromDelegate(ArtifactClassLoader artifactClassLoader, String name) throws IOException {
    return artifactClassLoader.findResources(name);
  }

  @Override
  public URL findResource(String name) {
    return artifactClassLoader.findResource(name);
  }

  @Override
  public Enumeration<URL> findResources(String name) throws IOException {
    return artifactClassLoader.findResources(name);
  }

  @Override
  public Class<?> findLocalClass(String name) throws ClassNotFoundException {
    return artifactClassLoader.findLocalClass(name);
  }

  @Override
  public String toString() {
    return format("%s[%s]@%s", getClass().getName(), artifactClassLoader.getArtifactName(), toHexString(identityHashCode(this)));
  }

  @Override
  public String getArtifactName() {
    return artifactClassLoader.getArtifactName();
  }

  @Override
  public ClassLoader getClassLoader() {
    return this;
  }

  @Override
  public void addShutdownListener(ShutdownListener listener) {
    artifactClassLoader.addShutdownListener(listener);
  }

  @Override
  public ClassLoaderLookupPolicy getClassLoaderLookupPolicy() {
    return artifactClassLoader.getClassLoaderLookupPolicy();
  }

  @Override
  public void dispose() {
    // Nothing to do here as this is just wrapper for another classLoader
  }

  @Override
  public URL findLocalResource(String resourceName) {
    return artifactClassLoader.findLocalResource(resourceName);
  }
}
