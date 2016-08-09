/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.sftp.command;

import org.mule.extension.ftp.internal.sftp.connection.SftpFileSystem;
import org.mule.extension.ftp.internal.sftp.connection.SftpClient;
import org.mule.runtime.module.extension.file.api.FileAttributes;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.command.DeleteCommand;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SftpCommand} which implements the {@link DeleteCommand} contract
 *
 * @since 4.0
 */
public final class SftpDeleteCommand extends SftpCommand implements DeleteCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(SftpDeleteCommand.class);

  /**
   * {@inheritDoc}
   */
  public SftpDeleteCommand(SftpFileSystem fileSystem, SftpClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(FileConnectorConfig config, String filePath) {
    FileAttributes fileAttributes = getExistingFile(config, filePath);
    final boolean isDirectory = fileAttributes.isDirectory();
    final String path = fileAttributes.getPath();

    if (isDirectory) {
      deleteDirectory(path);
    } else {
      deleteFile(path);
    }

    logDelete(path);
  }

  private void deleteFile(String path) {
    fileSystem.verifyNotLocked(Paths.get(path));
    LOGGER.debug("Preparing to delete file '{}'", path);
    client.deleteFile(path);
  }

  private void deleteDirectory(String path) {
    LOGGER.debug("Preparing to delete directory '{}'", path);
    for (FileAttributes file : client.list(path)) {
      final String filePath = file.getPath();
      if (isVirtualDirectory(file.getName())) {
        continue;
      }

      if (file.isDirectory()) {
        deleteDirectory(filePath);
      } else {
        deleteFile(filePath);
      }
    }

    Path directoryPath = Paths.get(path);
    Path directoryFragment = directoryPath.getName(directoryPath.getNameCount() - 1);
    if (isVirtualDirectory(directoryFragment.getFileName().toString())) {
      path = Paths.get("/").resolve(directoryPath.subpath(0, directoryPath.getNameCount() - 1)).toAbsolutePath().toString();
    }
    client.deleteDirectory(path);

    logDelete(path);
  }

  private void logDelete(String path) {
    LOGGER.debug("Successfully deleted '{}'", path);
  }
}
