/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static java.io.File.separator;

public class FilenameUtils {

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


