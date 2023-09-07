/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.artifact;

import static java.util.function.UnaryOperator.identity;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LibraryByJavaVersion {

  private static final Pattern DASH_VERSION = compile("-(\\d+(\\.|$))");
  private static final Pattern NON_ALPHANUM = compile("[^A-Za-z0-9]");
  private static final Pattern REPEATING_DOTS = compile("(\\.)(\\1)+");
  private static final Pattern LEADING_DOTS = compile("^\\.");
  private static final Pattern TRAILING_DOTS = compile("\\.$");

  private final int javaVersion;
  private final String libraryName;
  private final File jarFile;

  public LibraryByJavaVersion(int javaVersion, File jarFile) {
    this.javaVersion = javaVersion;
    this.libraryName = libraryNameFromJarFileName(jarFile);
    this.jarFile = jarFile;
  }

  public int getJavaVersion() {
    return javaVersion;
  }

  public String getLibraryName() {
    return libraryName;
  }

  public File getJarFile() {
    return jarFile;
  }

  public static List<File> resolveJvmDependantLibs(int javaVersionToFilter, Collection<LibraryByJavaVersion> libsByJavaVersion) {
    return libsByJavaVersion.stream()
        .filter(l -> javaVersionToFilter >= l.getJavaVersion())
        .sorted((l1, l2) -> {
          if (l1.getLibraryName().compareTo(l2.getLibraryName()) == 0) {
            return l1.javaVersion - l2.javaVersion;
          } else {
            return l1.getLibraryName().compareTo(l2.getLibraryName());
          }
        })
        .collect(toMap(LibraryByJavaVersion::getLibraryName, identity(), (v1, v2) -> v2))
        .values()
        .stream()
        .map(LibraryByJavaVersion::getJarFile)
        .collect(toList());
  }

  private static String libraryNameFromJarFileName(File libFile) {
    // Derive the version, and the module name if needed, from JAR file name
    String fn = libFile.getName();

    // drop ".jar"
    String name = fn.substring(0, fn.length() - 4);

    // find first occurrence of -${NUMBER}. or -${NUMBER}$
    Matcher matcher = DASH_VERSION.matcher(name);
    if (matcher.find()) {
      name = name.substring(0, matcher.start());
    }

    return cleanModuleName(name);
  }

  /**
   * Clean up candidate module name derived from a JAR file name.
   */
  private static String cleanModuleName(String mn) {
    // replace non-alphanumeric
    mn = NON_ALPHANUM.matcher(mn).replaceAll(".");

    // collapse repeating dots
    mn = REPEATING_DOTS.matcher(mn).replaceAll(".");

    // drop leading dots
    if (!mn.isEmpty() && mn.charAt(0) == '.')
      mn = LEADING_DOTS.matcher(mn).replaceAll("");

    // drop trailing dots
    int len = mn.length();
    if (len > 0 && mn.charAt(len - 1) == '.')
      mn = TRAILING_DOTS.matcher(mn).replaceAll("");

    return mn;
  }

}
