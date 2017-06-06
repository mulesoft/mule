/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_BASE_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Standalone server instance utility methods.
 * <p>
 * This utility methods are not meant to be used in container mode.
 * 
 * @since 4.0
 */
public class StandaloneServerUtils {

  /**
   * @return the MULE_HOME directory of this instance. Returns null if the property is not set
   */
  public static Optional<File> getMuleHome() {
    final String muleHome = System.getProperty(MULE_HOME_DIRECTORY_PROPERTY);
    return ofNullable(muleHome != null ? new File(muleHome) : null);
  }

  /**
   * The mule runtime base folder is a directory similar to the mule runtime installation one but with only the specific
   * configuration parts of the mule runtime installation such as the apps folder, the domain folder, the conf folder.
   *
   * @return the MULE_BASE directory of this instance. Returns the
   *         {@link org.mule.runtime.core.api.config.MuleProperties#MULE_HOME_DIRECTORY_PROPERTY} property value if
   *         {@link org.mule.runtime.core.api.config.MuleProperties#MULE_BASE_DIRECTORY_PROPERTY} is not set which may be null.
   */
  public static Optional<File> getMuleBase() {
    Optional<File> muleBase = null;
    String muleBaseVar = System.getProperty(MULE_BASE_DIRECTORY_PROPERTY);

    if (muleBaseVar != null && !muleBaseVar.trim().equals("") && !muleBaseVar.equals("%MULE_BASE%")) {
      try {
        muleBase = of(new File(muleBaseVar).getCanonicalFile());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    if (muleBase == null) {
      muleBase = getMuleHome();
    }
    return muleBase;
  }

}
