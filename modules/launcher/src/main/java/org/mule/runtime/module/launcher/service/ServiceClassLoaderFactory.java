/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.service;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 * Creates {@link ArtifactClassLoader} for service descriptors.
 */
public class ServiceClassLoaderFactory implements ArtifactClassLoaderFactory<ServiceDescriptor> {

  public static final String CLASSES_DIR = "classes";
  public static final String LIB_DIR = "lib";
  private static final String JAR_FILE = "*.jar";

  /**
   * @inherited
   */
  @Override
  public ArtifactClassLoader create(ArtifactClassLoader parent, ServiceDescriptor descriptor) {
    File rootFolder = descriptor.getRootFolder();
    if (rootFolder == null || !rootFolder.exists()) {
      throw new IllegalArgumentException("Service folder does not exists: " + (rootFolder != null ? rootFolder.getName() : null));
    }

    List<URL> urls = new LinkedList<>();

    addDirectoryToClassLoader(urls, new File(rootFolder, CLASSES_DIR));
    loadJarsFromFolder(urls, new File(rootFolder, LIB_DIR));

    return new MuleArtifactClassLoader(descriptor.getName(), urls.toArray(new URL[0]), parent.getClassLoader(),
                                       parent.getClassLoaderLookupPolicy());
  }

  private void loadJarsFromFolder(List<URL> urls, File folder) {
    if (!folder.exists()) {
      return;
    }

    FilenameFilter fileFilter = new WildcardFileFilter(JAR_FILE);
    File[] files = folder.listFiles(fileFilter);
    for (File jarFile : files) {
      urls.add(getFileUrl(jarFile));

    }
  }

  private URL getFileUrl(File jarFile) {
    try {
      return jarFile.toURI().toURL();
    } catch (MalformedURLException e) {
      // Should not happen as folder already exists
      throw new IllegalStateException("Cannot create service class loader", e);
    }
  }

  private void addDirectoryToClassLoader(List<URL> urls, File classesFolder) {
    if (classesFolder.exists()) {
      urls.add(getFileUrl(classesFolder));
    }
  }

}
