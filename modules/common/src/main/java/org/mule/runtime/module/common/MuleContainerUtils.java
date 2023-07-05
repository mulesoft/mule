/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.common;

import static org.mule.runtime.module.common.MuleProperties.MULE_BASE_DIRECTORY_PROPERTY;
import static org.mule.runtime.module.common.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;

import java.io.File;
import java.io.IOException;

public final class MuleContainerUtils {

  public static final String MULE_DOMAIN_FOLDER = "domains";
  private static final String MULE_APPS_FILENAME = "apps";
  private static final String MULE_LIB_FILENAME = "lib/mule";
  private static final String MULE_CONF_FILENAME = "conf";

  private MuleContainerUtils() {
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
   * @return the MULE_BASE directory of this instance. Returns the {@link MuleProperties#MULE_HOME_DIRECTORY_PROPERTY} property
   *         value if {@link MuleProperties#MULE_BASE_DIRECTORY_PROPERTY} is not set which may be null.
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
   * @return null if running embedded
   */
  public static File getMuleLibDir() {
    return isStandalone() ? new File(getMuleHome(), MULE_LIB_FILENAME) : null;
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

  /**
   * Sets {@code newClassLoader} as the context class loader for the {@code thread}, as long as said classloader is not the same
   * instance as {@code currentClassLoader}.
   * <p>
   * Since obtaining and setting the context classloader from a thread are expensive operations, the purpose of this method is to
   * avoid performing those operations when possible, which is why the two classloaders are tested not to be the same before
   * performing the set operation. For this method to make sense, {@code currentClassLoader} should actually be the current
   * context classloader from the {@code thread}.
   * <p>
   * This is how a typical use should look like:
   *
   * <pre>
   * Thread thread = Thread.currentThread();
   * ClassLoader currentClassLoader = thread.getContextClassLoader();
   * ClassLoader newClassLoader = getNewContextClassLoader(); // this one depends on your logic
   * ClassUtils.setContextClassLoader(thread, currentClassLoader, newClassLoader);
   * try {
   *   // execute your logic
   * } finally {
   *   // set things back as they were by reversing the arguments order
   *   ClassUtils.setContextClassLoader(thread, newClassLoader, currentClassLoader);
   * }
   * </pre>
   *
   * @param thread             the thread which context classloader is to be changed
   * @param currentClassLoader the thread's current context classloader
   * @param newClassLoader     the new classloader to be set
   * @since 4.3.0
   */
  public static void setContextClassLoader(Thread thread, ClassLoader currentClassLoader, ClassLoader newClassLoader) {
    if (currentClassLoader != newClassLoader) {
      thread.setContextClassLoader(newClassLoader);
    }
  }
}
