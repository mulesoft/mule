/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.internal.util;

import static java.io.File.separator;
import static java.io.File.separatorChar;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.filefilter.TrueFileFilter.INSTANCE;
import static org.apache.commons.io.filefilter.TrueFileFilter.TRUE;
import static org.apache.commons.lang3.ClassUtils.getPackageName;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Discovers Java packages from files and folders
 */
public class FileJarExplorer implements JarExplorer {

  protected static final String CLASS_EXTENSION = ".class";

  @Override
  public JarInfo explore(URI library) {
    Set<String> packages = new TreeSet<>();
    Set<String> resources = new TreeSet<>();

    try {
      final File libraryFile = new File(library);
      if (!libraryFile.exists()) {
        throw new IllegalArgumentException("Library file does not exists: " + library);
      }
      if (libraryFile.isDirectory()) {
        final Collection<File> files = listFiles(libraryFile, TRUE, INSTANCE);
        for (File classFile : files) {
          final String relativePath = classFile.getAbsolutePath().substring(libraryFile.getAbsolutePath().length() + 1);
          if (relativePath.endsWith(CLASS_EXTENSION)) {
            final String packageName =
                getPackageName(relativePath.substring(0, relativePath.length() - CLASS_EXTENSION.length()).replace(separator,
                                                                                                                   "."));
            packages.add(packageName);
          } else {
            if (separatorChar == '/') {
              resources.add(relativePath);
            } else {
              resources.add(relativePath.replace(separator, "/"));
            }
          }
        }
      } else {
        if (libraryFile.getName().toLowerCase().endsWith(".jar")) {

          try (ZipInputStream zip = new ZipInputStream(new FileInputStream(libraryFile))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
              if (entry.isDirectory()) {
                continue;
              }
              if (entry.getName().endsWith(CLASS_EXTENSION)) {
                final String packageName = getPackageName(entry.getName()
                    .substring(0, entry.getName().length() - CLASS_EXTENSION.length())
                    .replace("/", "."));
                packages.add(packageName);
              } else {
                resources.add(entry.getName());
              }
            }
          }
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Cannot explore URL: " + library, e);
    }

    return new JarInfo(packages, resources);
  }
}
