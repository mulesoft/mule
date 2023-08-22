/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.classlaoder.TestClassLoader;

public class TestArtifactClassLoader extends TestClassLoader implements ArtifactClassLoader {

  public TestArtifactClassLoader(ClassLoader parent) {
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
  public void dispose() {}

  @Override
  public URL findLocalResource(String resourceName) {
    return null;
  }

  @Override
  public URL findResource(String s) {
    return super.findResource(s);
  }

  @Override
  public URL findInternalResource(String resource) {
    return findResource(resource);
  }

  @Override
  public Enumeration<URL> findResources(String name) throws IOException {
    return super.findResources(name);
  }

  @Override
  public Class<?> findLocalClass(String name) throws ClassNotFoundException {
    return super.findClass(name);
  }

  @Override
  public Class<?> loadInternalClass(String name) throws ClassNotFoundException {
    return super.loadClass(name);
  }
}
