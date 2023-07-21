/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.test.util;


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
