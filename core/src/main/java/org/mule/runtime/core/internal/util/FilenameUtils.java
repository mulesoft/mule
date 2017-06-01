/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import org.mule.runtime.core.api.util.FileUtils;

import java.io.File;

public class FilenameUtils {

  public static File fileWithPathComponents(String[] pathComponents) {
    if (pathComponents == null) {
      return null;
    }

    StringBuilder buf = new StringBuilder(64);
    for (int i = 0; i < pathComponents.length; i++) {
      String component = pathComponents[i];
      if (component == null) {
        continue;
      }

      buf.append(component);
      if (i < pathComponents.length - 1) {
        buf.append(File.separator);
      }
    }
    return FileUtils.newFile(buf.toString());
  }

  /**
   * Never create instances of this class.
   */
  private FilenameUtils() {
    super();
  }
}


