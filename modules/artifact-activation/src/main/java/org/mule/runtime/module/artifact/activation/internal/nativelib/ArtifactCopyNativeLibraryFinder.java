/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.nativelib;

import static java.net.URLDecoder.decode;
import static java.nio.charset.Charset.defaultCharset;

import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.FilenameUtils.getName;
import static org.apache.commons.lang3.SystemUtils.IS_OS_MAC;

import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * * Finds native libraries in an application's lib folder and creates a copy of each found library inside a temporal application
 * folder.
 */
public class ArtifactCopyNativeLibraryFinder implements NativeLibraryFinder {

  protected Logger logger = LoggerFactory.getLogger(getClass());
  protected static final String JNILIB_EXTENSION = ".jnilib";
  public static final String EMPTY_STRING = "";
  public static final String LIB_PREFIX = "lib";
  public static final String HYPHEN_SEPARATOR = "-";
  private final File artifactTempFolder;
  private final URL[] urls;

  /**
   * Creates a new native library finder
   *
   * @param tempFolder folder where native lib files should be temporarily installed on each deployment.
   * @param urls       all the URLs that are included in the artifact classloader
   */
  public ArtifactCopyNativeLibraryFinder(File tempFolder, URL[] urls) {
    this.urls = urls;
    this.artifactTempFolder = new File(tempFolder, "native");

    if (this.artifactTempFolder.exists()) {
      cleanNativeLibs();
    } else {
      if (!this.artifactTempFolder.mkdirs()) {
        throw new IllegalStateException(String.format("Unable to create application '%s' folder",
                                                      this.artifactTempFolder.getAbsolutePath()));
      }
    }
  }

  @Override
  public String findLibrary(String name, String parentLibraryPath) {
    String libraryPath = parentLibraryPath;

    if (null == libraryPath) {
      libraryPath = findLibraryLocally(name);
    }

    if (libraryPath != null) {
      logger.debug("Found native library for '{}' on '{}'", name, libraryPath);

      final File tempLibrary = copyNativeLibrary(name, libraryPath);
      libraryPath = tempLibrary.getAbsolutePath();

      logger.debug("Created native library copy for '{}' on '{}'", name, libraryPath);
    }
    return libraryPath;
  }

  private void cleanNativeLibs() {
    String[] list = artifactTempFolder.list();

    if (list != null) {
      for (String library : list) {
        new File(artifactTempFolder, library).delete();
      }
    }
  }

  private File copyNativeLibrary(String name, String libraryPath) {
    final String nativeLibName = System.mapLibraryName(name);
    final File tempLibrary = new File(artifactTempFolder, nativeLibName + System.currentTimeMillis());

    try {
      final File library = new File(decode(libraryPath, defaultCharset().name()));
      copyFile(library, tempLibrary);

      return tempLibrary;
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Unable to generate copy for native library '%s' at '%s'", nativeLibName,
                                                    tempLibrary.getAbsolutePath()),
                                      e);
    }
  }

  private String findLibraryLocally(String libraryName) {
    String localLibraryNameWeakMatch = null;

    for (URL url : this.urls) {
      try {
        String localLibraryNameFullPath = url.toURI().toString();
        String extension = getExtension(System.mapLibraryName(libraryName));
        if (endsWithExtension(localLibraryNameFullPath, extension)) {
          if (isAStrictMatch(libraryName, localLibraryNameFullPath)) {
            return url.getFile();
          } else if (localLibraryNameWeakMatch == null && isAWeakMatch(libraryName, localLibraryNameFullPath)) {
            // The purpose of this if branch is to maintain backwards compatibility with the previous behaviour.
            localLibraryNameWeakMatch = url.getFile();
          }
        }
      } catch (URISyntaxException ignored) {
        // Next url.
      }
    }
    return localLibraryNameWeakMatch;
  }

  private static boolean endsWithExtension(String localLibraryNameFullPath, String extension) {
    return localLibraryNameFullPath.endsWith(extension) || IS_OS_MAC && localLibraryNameFullPath.endsWith(JNILIB_EXTENSION);
  }

  private static boolean isAStrictMatch(String libraryName, String localLibraryNameFullPath) {
    return getLibraryBaseName(localLibraryNameFullPath).equals(libraryName);
  }

  private static boolean isAWeakMatch(String libraryName, String localLibraryNameFullPath) {
    return getBaseName(localLibraryNameFullPath).contains(libraryName);
  }

  public List<String> findLibraryNames() {
    List<String> nativeLibraries = new ArrayList<>();
    for (URL url : this.urls) {
      String fullPath;
      try {
        fullPath = url.toURI().toString();
      } catch (URISyntaxException e) {
        throw new MuleRuntimeException(e);
      }
      String libraryName = getName(fullPath);
      if (isNativeLibrary(libraryName)) {
        String libraryBaseName = getLibraryBaseName(libraryName);
        nativeLibraries.add(libraryBaseName);
      }
    }
    return nativeLibraries;
  }

  private boolean isNativeLibrary(String libName) {
    if (Objects.equals(libName, EMPTY_STRING)) {
      return false;
    }

    for (NativeLibraryFileExtension extension : NativeLibraryFileExtension.values()) {
      if (libName.endsWith(extension.value())) {
        return true;
      }
    }
    return false;
  }

  private static String getLibraryBaseName(String libraryName) {
    String libraryBaseName = getBaseName(libraryName);
    if (libraryBaseName.startsWith(LIB_PREFIX)) {
      libraryBaseName = libraryBaseName.replaceFirst(LIB_PREFIX, EMPTY_STRING);
    }
    return libraryBaseName.split("-.*[0-9].*")[0];
  }
}
