/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.reboot;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_BASE_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;

import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.api.util.FileUtils;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;

public final class MuleContainerBootstrapUtils {

  public static final String MULE_DOMAIN_FOLDER = "domains";
  public static final String MULE_LOCAL_JAR_FILENAME = "mule-local-install.jar";
  private static final String MULE_APPS_FILENAME = "apps";
  private static final String MULE_LIB_FILENAME = "lib/mule";
  private static final String MULE_TMP_FILENAME = "tmp";
  private static final String MULE_CONF_FILENAME = "conf";

  private MuleContainerBootstrapUtils() {
    // utility class only
  }

  /**
   * Whether Mule is running embedded or standalone.
   * 
   * @return true if running standalone
   */
  public static boolean isStandalone() {
    // when embedded, mule.home var is not set
    return getMuleHome() != null;
  }

  /**
   * @return the mule runtime installation folder, null if running embedded
   */
  public static File getMuleHome() {
    final String muleHome = System.getProperty(MULE_HOME_DIRECTORY_PROPERTY);
    return muleHome != null ? new File(muleHome) : null;
  }

  /**
   * The mule runtime base folder is a directory similar to the mule runtime installation one but with only the specific
   * configuration parts of the mule runtime installation such as the apps folder, the domain folder, the conf folder.
   * 
   * @return the MULE_BASE directory of this instance. Returns the
   *         {@link org.mule.runtime.core.api.config.MuleProperties#MULE_HOME_DIRECTORY_PROPERTY} property value if
   *         {@link org.mule.runtime.core.api.config.MuleProperties#MULE_BASE_DIRECTORY_PROPERTY} is not set which may be null.
   */
  public static File getMuleBase() {
    File muleBase = null;
    String muleBaseVar = System.getProperty(MULE_BASE_DIRECTORY_PROPERTY);

    if (muleBaseVar != null && !muleBaseVar.trim().equals("") && !muleBaseVar.equals("%MULE_BASE%")) {
      try {
        muleBase = new File(muleBaseVar).getCanonicalFile();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    if (muleBase == null) {
      muleBase = getMuleHome();
    }
    return muleBase;
  }

  /**
   * @return null if running embedded, otherwise the apps dir as a File ref
   */
  public static File getMuleAppsDir() {
    return isStandalone() ? new File(getMuleBase(), MULE_APPS_FILENAME) : null;
  }

  /**
   * @param appName name of the application
   * @return null if running embedded, otherwise the app dir as a File ref
   */
  public static File getMuleAppDir(String appName) {
    return isStandalone() ? new File(getMuleAppsDir(), appName) : null;
  }

  /**
   * @return null if running embedded
   */
  public static File getMuleLibDir() {
    return isStandalone() ? new File(getMuleHome(), MULE_LIB_FILENAME) : null;
  }

  /**
   * @return null if running embedded, otherwise the $MULE_HOME/tmp dir reference
   */
  public static File getMuleTmpDir() {
    return isStandalone() ? new File(getMuleBase(), MULE_TMP_FILENAME) : null;
  }

  public static File getMuleLocalJarFile() {
    return isStandalone() ? new File(getMuleLibDir(), MULE_LOCAL_JAR_FILENAME) : null;
  }

  public static File getMuleDomainsDir() {
    return isStandalone() ? new File(getMuleBase(), MULE_DOMAIN_FOLDER) : null;
  }

  /**
   * @return null if running embedded, otherwise the conf dir as a File ref
   */
  public static File getMuleConfDir() {
    return isStandalone() ? new File(getMuleBase(), MULE_CONF_FILENAME) : null;
  }

  public static class ProxyInfo {

    String host;
    String port;
    String username;
    String password;

    public ProxyInfo(String host, String port) {
      this(host, port, null, null);
    }

    public ProxyInfo(String host, String port, String username, String password) {
      this.host = host;
      this.port = port;
      this.username = username;
      this.password = password;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////
  // The following methods are intentionally duplicated from org.mule.runtime.core.util so that
  // mule-module-reboot has no external dependencies at system startup.
  //////////////////////////////////////////////////////////////////////////////////////////

  /**
   * @see ClassUtils#getResource
   */
  public static URL getResource(final String resourceName, final Class<?> callingClass) {
    URL url = AccessController.doPrivileged((PrivilegedAction<URL>) () -> {
      final ClassLoader cl = Thread.currentThread().getContextClassLoader();
      return cl != null ? cl.getResource(resourceName) : null;
    });

    if (url == null) {
      url = AccessController
          .doPrivileged((PrivilegedAction<URL>) () -> MuleContainerBootstrap.class.getClassLoader().getResource(resourceName));
    }

    if (url == null) {
      url = AccessController.doPrivileged((PrivilegedAction<URL>) () -> callingClass.getClassLoader().getResource(resourceName));
    }

    return url;
  }

  /**
   * @see FileUtils#renameFile
   */
  public static boolean renameFile(File srcFile, File destFile) throws IOException {
    boolean isRenamed = false;
    if (srcFile != null && destFile != null) {
      if (!destFile.exists()) {
        if (srcFile.isFile()) {
          isRenamed = srcFile.renameTo(destFile);
          if (!isRenamed && srcFile.exists()) {
            isRenamed = renameFileHard(srcFile, destFile);
          }
        }
      }
    }
    return isRenamed;
  }

  /**
   * @see FileUtils#renameFileHard
   */
  public static boolean renameFileHard(File srcFile, File destFile) throws IOException {
    boolean isRenamed = false;
    if (srcFile != null && destFile != null) {
      if (!destFile.exists()) {
        if (srcFile.isFile()) {
          FileInputStream in = null;
          FileOutputStream out = null;
          try {
            in = new FileInputStream(srcFile);
            out = new FileOutputStream(destFile);
            out.getChannel().transferFrom(in.getChannel(), 0, srcFile.length());
            isRenamed = true;
          } finally {
            if (in != null) {
              in.close();
            }
            if (out != null) {
              out.close();
            }
          }
          if (isRenamed) {
            srcFile.delete();
          } else {
            destFile.delete();
          }
        }
      }
    }
    return isRenamed;
  }

  /**
   * @see IOUtils#copy
   */
  public static int copy(InputStream input, OutputStream output) throws IOException {
    long count = copyLarge(input, output);
    if (count > Integer.MAX_VALUE) {
      return -1;
    }
    return (int) count;
  }

  /**
   * @see IOUtils#copyLarge
   */
  public static long copyLarge(InputStream input, OutputStream output) throws IOException {
    byte[] buffer = new byte[1024 * 4];
    long count = 0;
    int n = 0;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }
}
