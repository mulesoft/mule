/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.classloader;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

public class TestArtifactClassLoader extends TestClassLoader implements ArtifactClassLoader {

  @Override
  public String getArtifactName() {
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
  public Enumeration<URL> findResources(String name) throws IOException {
    return super.findResources(name);
  }

  @Override
  public Class<?> findLocalClass(String name) throws ClassNotFoundException {
    return super.findClass(name);
  }
}
