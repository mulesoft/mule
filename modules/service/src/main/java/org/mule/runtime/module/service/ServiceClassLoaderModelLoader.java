/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service;

import static java.lang.String.format;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.SERVICE;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 * This class is responsible for loading the {@link ClassLoaderModelLoader} for a service artifact.
 *
 * @since 4.0
 */
public class ServiceClassLoaderModelLoader implements ClassLoaderModelLoader {

  static final String CLASSES_FOLDER = "classes";
  static final String LIB_FOLDER = "lib";

  private static final String CLASSES_DIR = "classes";
  private static final String LIB_DIR = "lib";
  private static final String JAR_FILE = "*.jar";

  @Override
  public String getId() {
    return "service";
  }

  @Override
  public ClassLoaderModel load(File artifactFile, Map<String, Object> attributes, ArtifactType artifactType)
      throws InvalidDescriptorLoaderException {
    if (artifactFile == null || !artifactFile.exists()) {
      throw new IllegalArgumentException("Service folder does not exists: "
          + (artifactFile != null ? artifactFile.getName() : null));
    }

    ClassLoaderModel.ClassLoaderModelBuilder classLoaderModelBuilder = new ClassLoaderModel.ClassLoaderModelBuilder();
    classLoaderModelBuilder.containing(getUrl(artifactFile));
    for (URL url : getServiceUrls(artifactFile)) {
      classLoaderModelBuilder.containing(url);
    }

    return classLoaderModelBuilder.build();
  }

  private URL getUrl(File file) {
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new ArtifactDescriptorCreateException(format("There was an exception obtaining the URL for the service: [%s]",
                                                         file.getAbsolutePath()),
                                                  e);
    }
  }

  private List<URL> getServiceUrls(File rootFolder) {
    List<URL> urls = new LinkedList<>();
    addDirectoryToClassLoader(urls, new File(rootFolder, CLASSES_DIR));
    loadJarsFromFolder(urls, new File(rootFolder, LIB_DIR));

    return urls;
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

  @Override
  public boolean supportsArtifactType(ArtifactType artifactType) {
    return SERVICE.equals(artifactType);
  }
}
