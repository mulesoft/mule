/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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


