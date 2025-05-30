/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_BASE_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;

import java.io.File;
import java.io.IOException;

public final class MuleContainerUtils {

  public static final String MULE_DOMAIN_FOLDER = "domains";
  public static final String MULE_LOCAL_JAR_FILENAME = "mule-local-install.jar";
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

}
