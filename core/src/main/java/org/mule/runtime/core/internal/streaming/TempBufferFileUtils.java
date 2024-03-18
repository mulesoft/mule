/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static java.nio.file.Files.createTempFile;

import java.io.File;
import java.io.IOException;

/**
 * Utility methods to handle temporal files
 *
 * @since 4.0
 */
public final class TempBufferFileUtils {

  /**
   * Creates a temporal file for buffering. The file is stored in the system temporal folder
   *
   * @param name a descriptive name. Not require to contain a path nor an extension
   * @return a {@link File}
   * @throws IOException
   */
  public static File createBufferFile(String name) throws IOException {
    return createTempFile("mule-buffer-" + name + "-", ".tmp").toFile();
  }

  private TempBufferFileUtils() {}
}
