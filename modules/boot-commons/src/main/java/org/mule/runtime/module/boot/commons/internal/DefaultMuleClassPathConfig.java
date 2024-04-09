/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.commons.internal;

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

  private static final String JAVA_8_VERSION = "1.8";
  private static final String JAVA_RUNNING_VERSION = "java.specification.version";

  protected static final String MULE_DIR = "/lib/mule";
  protected static final String USER_DIR = "/lib/user";
  protected static final String OPT_DIR = "/lib/opt";
  protected static final String OPT_JDK8_DIR = "/lib/opt/jdk-8";

  protected List<URL> muleUrls = new ArrayList<>();
  protected List<URL> optUrls = new ArrayList<>();

  public DefaultMuleClassPathConfig(File muleHome, File muleBase) {
    init(muleHome, muleBase);
  }

  protected void init(File muleHome, File muleBase) {
    /*
     * Pick up any local jars, if there are any. Doing this here insures that any local class that override the global classes
     * will in fact do so.
     */
    addMuleBaseUserLibs(muleHome, muleBase);

    addLibraryDirectory(muleUrls, muleHome, USER_DIR);
    addLibraryDirectory(muleUrls, muleHome, MULE_DIR);
    addLibraryDirectory(optUrls, muleHome, OPT_DIR);

    // Do not use commons-lang3 to avoid having to add that jar to lib/boot
    if (getProperty(JAVA_RUNNING_VERSION).startsWith(JAVA_8_VERSION)) {
      addLibraryDirectory(optUrls, muleHome, OPT_JDK8_DIR);
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
