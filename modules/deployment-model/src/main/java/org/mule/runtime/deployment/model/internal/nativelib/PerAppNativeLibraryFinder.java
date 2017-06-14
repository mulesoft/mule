/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.nativelib;

import static org.apache.commons.lang3.SystemUtils.IS_OS_MAC;

import java.io.File;

/**
 * Finds native libraries in an application's lib folder
 */
public class PerAppNativeLibraryFinder implements NativeLibraryFinder {

  public static final String DYLIB_EXTENSION = ".dylib";
  public static final String JNILIB_EXTENSION = ".jnilib";

  private final File libDir;

  public PerAppNativeLibraryFinder(File libDir) {
    this.libDir = libDir;
  }

  @Override
  public String findLibrary(String name, String libraryPath) {
    if (null == libraryPath) {
      libraryPath = findLibraryLocally(name);
    }

    return libraryPath;
  }

  protected String findLibraryLocally(String name) {
    String nativeLibName = System.mapLibraryName(name);
    File library = new File(libDir, nativeLibName);

    if (!library.exists() && IS_OS_MAC) {
      nativeLibName = nativeLibName.replace(DYLIB_EXTENSION, JNILIB_EXTENSION);
      library = new File(libDir, nativeLibName);
    }

    if (library.exists()) {
      return library.getAbsolutePath();
    } else {
      return null;
    }
  }
}
