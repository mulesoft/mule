/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.classloader;

import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.ShutdownListener;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

class DelegatingArtifactClassLoader extends ClassLoader implements ArtifactClassLoader {

  static {
    registerAsParallelCapable();
  }

  public DelegatingArtifactClassLoader(ClassLoader parent) {
    super(parent);
  }

  @Override
  public String getArtifactId() {
    return null;
  }

  @Override
  public <T extends ArtifactDescriptor> T getArtifactDescriptor() {
    return null;
  }

  @Override
  public ClassLoader getClassLoader() {
    return this;
  }

  @Override
  public void addShutdownListener(ShutdownListener listener) {

  }

  @Override
  public ClassLoaderLookupPolicy getClassLoaderLookupPolicy() {
    return null;
  }

  @Override
  public void dispose() {
    LifecycleUtils.trackDisposedObject(this);
  }

  @Override
  public URL findLocalResource(String resourceName) {
    return getParent().getResource(resourceName);
  }

  @Override
  public URL findResource(String s) {
    return getParent().getResource(s);
  }

  @Override
  public URL findInternalResource(String resource) {
    return null;
  }

  @Override
  public Enumeration<URL> findResources(String name) throws IOException {
    return getParent().getResources(name);
  }

  @Override
  public Class<?> findLocalClass(String name) throws ClassNotFoundException {
    return getParent().loadClass(name);
  }

  @Override
  public Class<?> loadInternalClass(String name) throws ClassNotFoundException {
    return null;
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    return getParent().loadClass(name);
  }

  @Override
  public URL getResource(String name) {
    return getParent().getResource(name);
  }

  @Override
  public InputStream getResourceAsStream(String name) {
    return getParent().getResourceAsStream(name);
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    return getParent().getResources(name);
  }

}

