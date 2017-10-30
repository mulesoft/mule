/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.mule.runtime.core.api.util.FileUtils.createTempFile;

import java.io.File;

/**
 * Utility methods to handle temporal files
 *
 * @since 4.0
 */
public final class TempBufferFileUtils {

  /**
   * Creates a temporal file for buffering. The file is stored in the system temporal
   * folder
   *
   * @param name a descriptive name. Not require to contain a path nor an extension
   * @return a {@link File}
   */
  public static File createBufferFile(String name) {
    return createTempFile("mule-buffer-${" + name + "}-", ".tmp");
  }

  private TempBufferFileUtils() {}
}
