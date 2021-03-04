/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static java.io.File.separator;
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
        buf.append(separator);
      }
    }
    return FileUtils.newFile(buf.toString());
  }

  public static String normalizeDecodedPath(String resource, boolean isWindows) {
    if (resource == null) {
      return null;
    }

    if (!resource.startsWith("file:/")) {
      return resource;
    }

    // Remove the "file:/"
    resource = resource.substring(6);

    if (isWindows) {
      if (resource.startsWith("/")) {
        return separator + resource;
      }
    } else {
      if (!resource.startsWith(separator)) {
        return separator + resource;
      }
    }

    return resource;
  }

  /**
   * Never create instances of this class.
   */
  private FilenameUtils() {
    super();
  }
}


