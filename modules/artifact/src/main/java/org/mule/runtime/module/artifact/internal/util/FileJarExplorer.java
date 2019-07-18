/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.internal.util;

import static java.io.File.separator;
import static java.io.File.separatorChar;
import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.filefilter.TrueFileFilter.INSTANCE;
import static org.apache.commons.io.filefilter.TrueFileFilter.TRUE;

import org.mule.runtime.module.artifact.api.classloader.ExportedService;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Discovers Java packages from files and folders
 */
public class FileJarExplorer implements JarExplorer {

  protected static final String CLASS_EXTENSION = ".class";
  private static final String META_INF_SERVICES_PATH = "META-INF/services/";

  private static final Pattern SLASH_PATTERN = compile("/");
  private static final Pattern SEPARATOR_PATTERN = compile(quote(separator));

  @Override
  public JarInfo explore(URI library) {
    Set<String> packages = new TreeSet<>();
    Set<String> resources = new TreeSet<>();
    List<ExportedService> services = new ArrayList<>();

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
            packages.add(SEPARATOR_PATTERN
                .matcher(relativePath.substring(0, relativePath.lastIndexOf(separatorChar)))
                .replaceAll("."));
          } else {
            if (separatorChar == '/') {
              resources.add(relativePath);
            } else {
              resources.add(SEPARATOR_PATTERN.matcher(relativePath).replaceAll("/"));
            }
          }
        }
      } else {
        if (libraryFile.getName().toLowerCase().endsWith(".jar")) {

          try (final ZipFile zipFile = new ZipFile(libraryFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
              final ZipEntry entry = entries.nextElement();
              final String name = entry.getName();

              if (entry.isDirectory()) {
                continue;
              } else if (name.startsWith(META_INF_SERVICES_PATH)) {
                String serviceInterface = name.substring(META_INF_SERVICES_PATH.length());
                URL resource = getServiceResourceUrl(libraryFile.toURI().toURL(), name);

                services.add(new ExportedService(serviceInterface, resource));
              } else if (name.endsWith(CLASS_EXTENSION)) {
                if (name.lastIndexOf('/') < 0) {
                  // skip default package
                  continue;
                }

                packages.add(SLASH_PATTERN
                    .matcher(name.substring(0, name.lastIndexOf('/')))
                    .replaceAll("."));
              } else {
                resources.add(name);
              }
            }
          }
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Cannot explore URL: " + library, e);
    }

    return new JarInfo(packages, resources, services);
  }

  public static URL getServiceResourceUrl(URL resource, String serviceInterface) throws MalformedURLException {
    return new URL("jar:" + resource + "!/" + serviceInterface);
  }
}
