/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.application;

import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.lang.ClassUtils.getPackageName;
import static org.mule.runtime.module.launcher.application.MuleApplicationClassLoaderFactory.CLASS_EXTENSION;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Discovers Java packages from files and folders
 */
public class FilePackageDiscoverer implements PackageDiscoverer {

  @Override
  public Set<String> findPackages(URL library) {
    Set<String> packageNames = new HashSet<>();
    try {
      final File libraryFile = new File(URLDecoder.decode(library.getFile(), "UTF-8"));
      if (!libraryFile.exists()) {
        throw new IllegalArgumentException("Library file does not exists: " + library);
      }
      if (libraryFile.isDirectory()) {
        final Collection<File> classFiles = listFiles(libraryFile, new String[] {"class"}, true);
        for (File classFile : classFiles) {
          final String relativePath = classFile.getAbsolutePath().substring(libraryFile.getAbsolutePath().length() + 1);
          final String packageName =
              getPackageName(relativePath.substring(0, relativePath.length() - CLASS_EXTENSION.length()).replace("/", "."));
          packageNames.add(packageName);
        }
      } else {
        if (!libraryFile.getName().toLowerCase().endsWith(".jar")) {
          return Collections.EMPTY_SET;
        }

        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(libraryFile))) {
          for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            if (!entry.isDirectory() && entry.getName().endsWith(CLASS_EXTENSION)) {
              final String packageName = getPackageName(entry.getName()
                  .substring(0, entry.getName().length() - CLASS_EXTENSION.length()).replace("/", "."));
              packageNames.add(packageName);
            }
          }
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Cannot explore URL: " + library, e);
    }

    return packageNames;
  }
}
