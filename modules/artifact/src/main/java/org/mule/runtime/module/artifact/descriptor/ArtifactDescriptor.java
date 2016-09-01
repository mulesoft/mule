/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.descriptor;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter;

import java.io.File;

public class ArtifactDescriptor {

  private String name;
  private File rootFolder;
  private ArtifactClassLoaderFilter classLoaderFilter = DefaultArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public File getRootFolder() {
    return rootFolder;
  }

  public void setRootFolder(File rootFolder) {
    if (rootFolder == null) {
      throw new IllegalArgumentException("Root folder cannot be null");
    }

    this.rootFolder = rootFolder;
  }

  public ArtifactClassLoaderFilter getClassLoaderFilter() {
    return classLoaderFilter;
  }

  public void setClassLoaderFilter(ArtifactClassLoaderFilter classLoaderFilter) {
    this.classLoaderFilter = classLoaderFilter;
  }
}
