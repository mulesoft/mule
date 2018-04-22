/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static java.io.File.pathSeparatorChar;
import static java.lang.String.format;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.module.artifact.api.classloader.ExportedService;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Explores the content of the JRE used to run the container.
 */
public final class JreExplorer {

  private static final String META_INF_SERVICES_PATH = "META-INF/services/";

  private JreExplorer() {}

  /**
   * Explores the content of the JRE being used
   *
   * @param packages will store the Java packages found on the environment. Non null.
   * @param resources will store the resources found on the environment. Non null.
   * @param services will store the services defined via SPI found on the environment. Non null.
   */
  public static void exploreJdk(final Set<String> packages, Set<String> resources, List<ExportedService> services) {
    List<String> jdkPaths = new ArrayList<>();
    jdkPaths.add(System.getProperty("sun.boot.class.path"));
    jdkPaths.add(System.getProperty("java.ext.dirs"));

    explorePaths(jdkPaths, packages, resources, services);
  }

  /**
   * Explores the provided paths searching for Java packages, resources and SPI service definitions
   *
   * @param jdkPaths paths to explore. Non null.
   * @param packages will store the Java packages found on the environment. Non null.
   * @param resources will store the resources found on the environment. Non null.
   * @param services will store the services defined via SPI found on the environment. Non null.
   */
  static void explorePaths(final List<String> jdkPaths, final Set<String> packages, Set<String> resources,
                           List<ExportedService> services) {
    checkArgument(jdkPaths != null && !jdkPaths.isEmpty(), "jdkPaths cannot be empty");

    for (String jdkPath : jdkPaths) {
      if (jdkPath != null) {
        explorePath(packages, resources, services, jdkPath);
      }
    }
  }

  private static void explorePath(Set<String> packages, Set<String> resources, List<ExportedService> services, String jdkPath) {
    int fromIndex = 0;
    int endIndex;

    do {
      endIndex = jdkPath.indexOf(pathSeparatorChar, fromIndex);
      String item = endIndex == -1 ? jdkPath.substring(fromIndex) : jdkPath.substring(fromIndex, endIndex);

      final File file = new File(item);
      if (file.exists()) {
        if (file.isDirectory()) {
          exploreDirectory(packages, resources, services, file);
        } else {
          try {
            exploreJar(packages, resources, services, file);
          } catch (IOException e) {
            throw new IllegalStateException(createJarExploringError(file), e);
          }
        }
      }
      fromIndex = endIndex + 1;
    } while (endIndex != -1);
  }

  private static void exploreJar(Set<String> packages, Set<String> resources, List<ExportedService> services, File file)
      throws IOException {
    final ZipFile zipFile = new ZipFile(file);

    try {
      final Enumeration<? extends ZipEntry> entries = zipFile.entries();

      while (entries.hasMoreElements()) {
        final ZipEntry entry = entries.nextElement();
        final String name = entry.getName();
        final int lastSlash = name.lastIndexOf('/');
        if (lastSlash != -1 && name.endsWith(".class")) {
          packages.add(name.substring(0, lastSlash).replaceAll("/", "."));
        } else if (!entry.isDirectory()) {
          if (name.startsWith(META_INF_SERVICES_PATH)) {
            String serviceInterface = name.substring(META_INF_SERVICES_PATH.length());
            URL resource = getServiceResourceUrl(file.toURI().toURL(), name);

            services.add(new ExportedService(serviceInterface, resource));
          } else {
            resources.add(name);
          }
        }
      }
    } finally {
      if (zipFile != null)
        try {
          zipFile.close();
        } catch (Throwable ignored) {
        }
    }
  }

  private static void exploreDirectory(final Set<String> packages, Set<String> resources, List<ExportedService> services,
                                       final File file) {
    for (File entry : file.listFiles()) {
      if (entry.isDirectory()) {
        exploreDirectory(packages, resources, services, entry);
      } else if (entry.getName().endsWith(".jar")) {
        try {
          exploreJar(packages, resources, services, entry);
        } catch (IOException e) {
          throw new IllegalStateException(createJarExploringError(entry), e);
        }
      }
    }
  }

  private static String createJarExploringError(File file) {
    return format("Unable to explore '%s'", file.getAbsoluteFile());
  }

  static URL getServiceResourceUrl(URL resource, String serviceInterface) throws MalformedURLException {
    return new URL("jar:" + resource + "!/" + serviceInterface);
  }
}
