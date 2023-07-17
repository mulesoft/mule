/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.artifact;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.SERVER_PLUGIN;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.SERVICE;
import static org.mule.runtime.module.service.internal.artifact.LibraryByJavaVersion.resolveJvmDependantLibs;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import static org.apache.commons.lang3.SystemUtils.JAVA_VM_SPECIFICATION_VERSION;

import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration.ClassLoaderConfigurationBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 * This class is responsible for loading the {@link ClassLoaderConfigurationLoader} for artifacts that uses a lib folder to store
 * dependencies, like {@link ArtifactType#SERVICE} and {@link ArtifactType#SERVER_PLUGIN}
 *
 * @since 4.0
 */
public class LibFolderClassLoaderConfigurationLoader implements ClassLoaderConfigurationLoader {

  public static final String LIB_FOLDER = "lib";

  private static final Set<ArtifactType> supportedTypes = new HashSet<>(asList(SERVICE, SERVER_PLUGIN));

  private static final int JVM_SPECIFICATION_VERSION = parseInt(JAVA_VM_SPECIFICATION_VERSION.split("\\.")[0]);
  private static final String JAR_FILE = "*.jar";
  private static final FilenameFilter JAR_FILE_FILTER = WildcardFileFilter.builder().setWildcards(JAR_FILE).get();

  @Override
  public String getId() {
    return "service";
  }

  @Override
  public ClassLoaderConfiguration load(File artifactFile, Map<String, Object> attributes, ArtifactType artifactType)
      throws InvalidDescriptorLoaderException {
    if (artifactFile == null || !artifactFile.exists()) {
      throw new IllegalArgumentException("Service folder does not exists: "
          + (artifactFile != null ? artifactFile.getName() : null));
    }

    ClassLoaderConfigurationBuilder classLoaderConfigurationBuilder = new ClassLoaderConfigurationBuilder();
    classLoaderConfigurationBuilder.containing(getUrl(artifactFile));
    classLoaderConfigurationBuilder.containing(getServiceUrls(artifactFile));
    classLoaderConfigurationBuilder.containing(getServiceUrlsForJavaVersion(artifactFile));

    return classLoaderConfigurationBuilder.build();
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
    loadJarsFromFolder(urls, new File(rootFolder, LIB_FOLDER));
    return urls;
  }

  private List<URL> getServiceUrlsForJavaVersion(File rootFolder) {
    List<URL> urls = new LinkedList<>();

    final File libJavaVersionsDir = new File(rootFolder, LIB_FOLDER + "/java-versions");
    if (libJavaVersionsDir.exists()) {
      for (File jvmDependantLib : resolveJvmDependantLibs(JVM_SPECIFICATION_VERSION,
                                                          loadLibsByJavaVersion(libJavaVersionsDir))) {
        urls.add(getFileUrl(jvmDependantLib));
      }
    }

    return urls;
  }

  private Collection<LibraryByJavaVersion> loadLibsByJavaVersion(final File libJavaVersionsDir) {
    Set<LibraryByJavaVersion> libsByJavaVersion = new HashSet<>();

    for (File libJavaVersionDir : libJavaVersionsDir.listFiles()) {
      final int libsJavaVersion;
      try {
        libsJavaVersion = parseInt(libJavaVersionDir.getName());
      } catch (NumberFormatException e) {
        throw new IllegalStateException(format("Could not obtain a valid Java version from the folder name: %s",
                                               libJavaVersionDir.getAbsolutePath()),
                                        e);
      }
      for (File libJavaVersionJar : libJavaVersionDir.listFiles(JAR_FILE_FILTER)) {
        libsByJavaVersion.add(new LibraryByJavaVersion(libsJavaVersion, libJavaVersionJar));
      }
    }
    return libsByJavaVersion;
  }

  private void loadJarsFromFolder(List<URL> urls, File folder) {
    if (!folder.exists()) {
      return;
    }

    File[] files = folder.listFiles(JAR_FILE_FILTER);
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

  @Override
  public boolean supportsArtifactType(ArtifactType artifactType) {
    return supportedTypes.contains(artifactType);
  }
}
