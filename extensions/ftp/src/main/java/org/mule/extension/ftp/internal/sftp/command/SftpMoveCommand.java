/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.sftp.command;

import org.mule.extension.ftp.internal.sftp.connection.SftpFileSystem;
import org.mule.extension.ftp.internal.ftp.command.MoveFtpDelegate;
import org.mule.extension.ftp.internal.sftp.connection.SftpClient;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.command.MoveCommand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SftpCommand} which implements the {@link MoveCommand} contract
 *
 * @since 4.0
 */
public class SftpMoveCommand extends SftpCommand implements MoveCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(SftpMoveCommand.class);

  /**
   * {@inheritDoc}
   */
  public SftpMoveCommand(SftpFileSystem fileSystem, SftpClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void move(FileConnectorConfig config, String sourcePath, String targetPath, boolean overwrite,
                   boolean createParentDirectories) {
    copy(config, sourcePath, targetPath, overwrite, createParentDirectories, null, new MoveFtpDelegate(this, fileSystem));
    LOGGER.debug("Moved '{}' to '{}'", sourcePath, targetPath);
  }
}
