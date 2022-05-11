/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;


import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public class TestFileUtils {

  public static boolean isFileOpen(File file) {
    if (IS_OS_WINDOWS) {
      return isFileOpenWindows(file);
    } else {
      return isFileOpenUnix(file);
    }
  }

  static boolean isFileOpenUnix(File file) {
    String filePath = file.getAbsolutePath();
    File nullFile = new File("/dev/null");

    // use lsof utility
    ProcessBuilder builder =
        new ProcessBuilder("lsof", "-c", "java", "-a", "-T", "--", filePath).redirectError(nullFile).redirectOutput(nullFile);

    try {
      Process process = builder.start();
      int exitValue = process.waitFor();
      return exitValue == 0;
    } catch (InterruptedException | IOException e) {
      // ignore the exceptions
    }
    return false;
  }

  static boolean isFileOpenWindows(File file) {
    // try to rename to the same name will fail on Windows
    URI fileURI = file.toURI();
    File sameFileName = new File(fileURI);
    return !file.renameTo(sameFileName);
  }

}
