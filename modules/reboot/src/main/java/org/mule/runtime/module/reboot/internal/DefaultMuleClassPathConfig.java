/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.reboot.internal;

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
// TODO this duplicates DefaultMuleClassPathConfig in the boot module. See if this class can be moved to mule-core
public class DefaultMuleClassPathConfig {

  private static final String JAVA_8_VERSION = "1.8";
  private static final String JAVA_RUNNING_VERSION = "java.specification.version";

  protected static final String MULE_DIR = "/lib/mule";
  protected static final String USER_DIR = "/lib/user";
  protected static final String OPT_DIR = "/lib/opt";
  protected static final String OPT_JDK8_DIR = "/lib/opt/jdk-8";

  protected List<URL> urls = new ArrayList<>();

  public DefaultMuleClassPathConfig(File muleHome, File muleBase) {
    init(muleHome, muleBase);
  }

  protected void init(File muleHome, File muleBase) {
    /*
     * Pick up any local jars, if there are any. Doing this here insures that any local class that override the global classes
     * will in fact do so.
     */
    addMuleBaseUserLibs(muleHome, muleBase);

    addLibraryDirectory(muleHome, USER_DIR);
    addLibraryDirectory(muleHome, MULE_DIR);
    addLibraryDirectory(muleHome, OPT_DIR);

    // Do not use commons-lang3 to avoid having to add that jar to lib/boot
    if (getProperty(JAVA_RUNNING_VERSION).startsWith(JAVA_8_VERSION)) {
      addLibraryDirectory(muleHome, OPT_JDK8_DIR);
    }
  }

  protected void addMuleBaseUserLibs(File muleHome, File muleBase) {
    try {
      if (!muleHome.getCanonicalFile().equals(muleBase.getCanonicalFile())) {
        File userOverrideDir = new File(muleBase, USER_DIR);
        addFile(userOverrideDir);
        addFiles(listJars(userOverrideDir));
      }
    } catch (IOException ioe) {
      System.out.println("Unable to check to see if there are local jars to load: " + ioe.toString());
    }
  }

  protected void addLibraryDirectory(File muleHome, String libDirectory) {
    File directory = new File(muleHome, libDirectory);
    addFile(directory);
    addFiles(listJars(directory));
  }

  public List<URL> getURLs() {
    return new ArrayList<>(this.urls);
  }

  public void addURLs(List<URL> moreUrls) {
    if (moreUrls != null && !moreUrls.isEmpty()) {
      this.urls.addAll(moreUrls);
    }
  }

  /**
   * Add a URL to Mule's classpath.
   *
   * @param url folder (should end with a slash) or jar path
   */
  public void addURL(URL url) {
    this.urls.add(url);
  }

  public void addFiles(List<File> files) {
    for (File file : files) {
      this.addFile(file);
    }
  }

  public void addFile(File jar) {
    try {
      this.addURL(jar.getAbsoluteFile().toURI().toURL());
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
