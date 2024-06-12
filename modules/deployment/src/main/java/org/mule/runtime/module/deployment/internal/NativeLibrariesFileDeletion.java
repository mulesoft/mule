/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppNativeLibrariesTempFolder;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.util.Collection;

import org.slf4j.Logger;

public class NativeLibrariesFileDeletion {

  private final String applicationName;
  private final String[] nativeLibrariesExtensions = {"dylib", "dll", "jnilib", "so"};
  private static final Logger LOGGER = getLogger(NativeLibrariesFileDeletion.class);

  public NativeLibrariesFileDeletion(String applicationName) {
    this.applicationName = applicationName;
  }

  public void doAction() {
    File appNativeLibrariesTempFolder = getAppNativeLibrariesTempFolder(applicationName);
    Collection<File> nativeLibraries =
        listFiles(appNativeLibrariesTempFolder, nativeLibrariesExtensions, true);
    for (File nativeLib : nativeLibraries) {
      try {
        if (nativeLib.delete()) {
          LOGGER.info(format("Native library file deleted: '%s'", nativeLib.getAbsolutePath()));
        }
      } catch (Exception e) {
        LOGGER.warn(
                    format("Cannot delete native library '%s' while undeploying the artifact '%s'. This could be related to some files still being used and can cause a memory leak.",
                           nativeLib, applicationName));
      }
    }
  }
}
