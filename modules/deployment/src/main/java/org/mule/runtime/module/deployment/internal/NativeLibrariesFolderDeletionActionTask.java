/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.lang.String.format;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;

import org.slf4j.Logger;

public class NativeLibrariesFolderDeletionActionTask implements ActionTask {

  private final String applicationName;
  private final File appNativeLibrariesFolder;
  private static final Logger LOGGER = getLogger(NativeLibrariesFolderDeletionActionTask.class);

  public NativeLibrariesFolderDeletionActionTask(String applicationName, File appNativeLibrariesFolder) {
    this.applicationName = applicationName;
    this.appNativeLibrariesFolder = appNativeLibrariesFolder;
  }

  @Override
  public boolean tryAction() {
    boolean actionPerformed = true;

    try {
      deleteDirectory(appNativeLibrariesFolder);
      LOGGER.debug("App Native Libraries folder deleted: {}", appNativeLibrariesFolder.getAbsolutePath());
    } catch (Throwable e) {
      LOGGER
          .warn(format("Cannot delete App Native Libraries folder '%s' from artifact '%s'. This could be related to some files still being used. Exception: %s",
                       appNativeLibrariesFolder, applicationName, e.getMessage()));
      actionPerformed = false;
    }

    return actionPerformed;
  }
}
