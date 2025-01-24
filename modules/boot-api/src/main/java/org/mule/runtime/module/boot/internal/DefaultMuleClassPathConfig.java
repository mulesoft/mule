/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.internal;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Constructs a default set of JAR Urls located under Mule home folder.
 */
public class DefaultMuleClassPathConfig {

  private static final String JAVA_RUNNING_VERSION = "java.specification.version";

  protected static final String MULE_DIR = "/lib/mule";
  protected static final String MULE_JAVA_SPECIFIC_DIR = "/lib/mule/jdk-%s";
  protected static final String USER_DIR = "/lib/user";
  protected static final String OPT_DIR = "/lib/opt";
  protected static final String OPT_JAVA_SPECIFIC_DIR = "/lib/opt/jdk-%s";

  protected List<URL> muleUrls = new ArrayList<>();
  protected List<URL> optUrls = new ArrayList<>();
  protected List<URL> resourceUrls = new ArrayList<>();

  public DefaultMuleClassPathConfig(File muleHome, File muleBase) {
    init(muleHome, muleBase);
  }

  protected void init(File muleHome, File muleBase) {
    /*
     * Pick up any local jars, if there are any. Doing this here insures that any local class that override the global classes
     * will in fact do so.
     */
    addMuleBaseUserLibs(muleHome, muleBase);

    addLibraryDirectory(muleUrls, muleHome, MULE_DIR);
    addLibraryDirectory(optUrls, muleHome, USER_DIR);
    addLibraryDirectory(optUrls, muleHome, OPT_DIR);

    // Add resources paths. This is needed when using jdk 17 which uses module layers instead of classpath with urls.
    addFile(resourceUrls, new File(muleHome, USER_DIR));

    // Support adding extra libs for specific java versions
    final var javaSpecVersion = getProperty(JAVA_RUNNING_VERSION).split("\\.")[0];

    final var muleJdkSpecificDir = format(MULE_JAVA_SPECIFIC_DIR, javaSpecVersion);
    if (new File(muleHome, muleJdkSpecificDir).exists()) {
      addLibraryDirectory(muleUrls, muleHome, muleJdkSpecificDir);
    }
    final var optJdkSpecificDir = format(OPT_JAVA_SPECIFIC_DIR, javaSpecVersion);
    if (new File(muleHome, optJdkSpecificDir).exists()) {
      addLibraryDirectory(optUrls, muleHome, optJdkSpecificDir);
    }
  }

  protected void addMuleBaseUserLibs(File muleHome, File muleBase) {
    try {
      if (!muleHome.getCanonicalFile().equals(muleBase.getCanonicalFile())) {
        File userOverrideDir = new File(muleBase, USER_DIR);
        addFile(muleUrls, userOverrideDir);
        addFiles(muleUrls, listJars(userOverrideDir));
      }
    } catch (IOException ioe) {
      System.out.println("Unable to check to see if there are local jars to load: " + ioe.toString());
    }
  }

  protected void addLibraryDirectory(List<URL> urls, File muleHome, String libDirectory) {
    File directory = new File(muleHome, libDirectory);
    addFile(urls, directory);
    addFiles(urls, listJars(directory));
  }

  public List<URL> getMuleURLs() {
    return new ArrayList<>(this.muleUrls);
  }

  public List<URL> getOptURLs() {
    return new ArrayList<>(this.optUrls);
  }

  public List<URL> getResourceURLs() {
    return new ArrayList<>(this.resourceUrls);
  }

  /**
   * Add a URL to Mule's classpath.
   *
   * @param url folder (should end with a slash) or jar path
   */
  public void addURL(List<URL> urls, URL url) {
    urls.add(url);
  }

  public void addFiles(List<URL> urls, List<File> files) {
    for (File file : files) {
      this.addFile(urls, file);
    }
  }

  public void addFile(List<URL> urls, File jar) {
    try {
      this.addURL(urls, jar.getAbsoluteFile().toURI().toURL());
    } catch (MalformedURLException mux) {
      throw new RuntimeException("Failed to construct a classpath URL", mux);
    }
  }

  /**
   * Find and if necessary filter the jars for classpath.
   *
   * @return a list of {@link java.io.File}s
   */
  protected List<File> listJars(File path) {
    File[] jars = path.listFiles((FileFilter) pathname -> {
      try {
        return pathname.getCanonicalPath().endsWith(".jar");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    if (jars != null) {
      return asList(jars);
    }
    return emptyList();
  }

}
