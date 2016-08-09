/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp.command;

import org.mule.extension.ftp.internal.ftp.connection.ClassicFtpFileSystem;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.command.MoveCommand;

import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ClassicFtpCommand} which implements the {@link MoveCommand} contract
 *
 * @since 4.0
 */
public final class FtpMoveCommand extends ClassicFtpCommand implements MoveCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(FtpMoveCommand.class);

  /**
   * {@inheritDoc}
   */
  public FtpMoveCommand(ClassicFtpFileSystem fileSystem, FTPClient client) {
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
