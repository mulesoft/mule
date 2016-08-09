/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.sftp.command;

import org.mule.extension.ftp.internal.sftp.connection.SftpFileSystem;
import org.mule.extension.ftp.internal.sftp.connection.SftpClient;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.command.CreateDirectoryCommand;

/**
 * A {@link SftpCommand} which implements the {@link CreateDirectoryCommand} contract
 *
 * @since 4.0
 */
public final class SftpCreateDirectoryCommand extends SftpCommand implements CreateDirectoryCommand {

  /**
   * {@inheritDoc}
   */
  public SftpCreateDirectoryCommand(SftpFileSystem fileSystem, SftpClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createDirectory(FileConnectorConfig config, String directoryName) {
    super.createDirectory(config, directoryName);
  }
}
