/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.container.internal;

import static org.mule.runtime.jpms.api.JpmsUtils.exploreJdkModules;

import static java.io.File.pathSeparatorChar;
import static java.lang.System.getProperties;
import static java.lang.System.getProperty;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.module.artifact.api.classloader.ExportedService;
import org.mule.runtime.module.artifact.internal.util.FileJarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarExplorer;
import org.mule.runtime.module.artifact.internal.util.JarInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

/**
 * Explores the content of the JRE used to run the container.
 */
public final class JreExplorer {

  private static final Logger LOGGER = getLogger(JreExplorer.class);

  private static final JarExplorer jarExplorer;

  static {
    jarExplorer = new FileJarExplorer();
  }

  private JreExplorer() {}

  /**
   * Explores the content of the JRE being used
   *
   * @param packages  will store the Java packages found on the environment. Non null.
   * @param resources will store the resources found on the environment. Non null.
   * @param services  will store the services defined via SPI found on the environment. Non null.
   */
  public static void exploreJdk(final Set<String> packages, Set<String> resources, List<ExportedService> services) {
    List<String> jdkPaths = new ArrayList<>();

    // These are present in JDK 8
    addJdkPath(jdkPaths, "sun.boot.class.path");
    addJdkPath(jdkPaths, "java.ext.dirs");

    // These is present in JDK 9, 10, 11
    addJdkPath(jdkPaths, "sun.boot.library.path");

    if (jdkPaths.isEmpty()) {
      LOGGER.warn("No JDK path/dir system property found. Defaulting to the whole classpath."
          + " This may cause classloading issues in some plugins.");
      jdkPaths.add(getProperty("java.class.path"));
    }

    explorePaths(jdkPaths, packages, resources, services);
    exploreJdkModules(packages);
  }

  private static void addJdkPath(List<String> jdkPaths, String key) {
    if (getProperties().containsKey(key)) {
      jdkPaths.add(getProperty(key));
    }
  }

  /**
   * Explores the provided paths searching for Java packages, resources and SPI service definitions
   *
   * @param jdkPaths  paths to explore. Non null.
   * @param packages  will store the Java packages found on the environment. Non null.
   * @param resources will store the resources found on the environment. Non null.
   * @param services  will store the services defined via SPI found on the environment. Non null.
   */
  static void explorePaths(final List<String> jdkPaths, final Set<String> packages, Set<String> resources,
                           List<ExportedService> services) {
    if (jdkPaths == null || jdkPaths.isEmpty()) {
      throw new IllegalArgumentException("jdkPaths cannot be empty");
    }

    for (String jdkPath : jdkPaths) {
      explorePath(packages, resources, services, jdkPath);
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
          final JarInfo exploredJar = jarExplorer.explore(file.toURI());

          packages.addAll(exploredJar.getPackages());
          resources.addAll(exploredJar.getResources());
          services.addAll(exploredJar.getServices());
        }
      }
      fromIndex = endIndex + 1;
    } while (endIndex != -1);
  }

  private static void exploreDirectory(final Set<String> packages, Set<String> resources, List<ExportedService> services,
                                       final File file) {
    File[] content = file.listFiles();
    if (content == null) {
      return;
    }

    for (File entry : content) {
      if (entry.exists()) {
        if (entry.isDirectory()) {
          exploreDirectory(packages, resources, services, entry);
        } else if (entry.getName().endsWith(".jar")) {
          final JarInfo exploredJar = jarExplorer.explore(entry.toURI());

          packages.addAll(exploredJar.getPackages());
          resources.addAll(exploredJar.getResources());
          services.addAll(exploredJar.getServices());
        }
      }
    }
  }
}
