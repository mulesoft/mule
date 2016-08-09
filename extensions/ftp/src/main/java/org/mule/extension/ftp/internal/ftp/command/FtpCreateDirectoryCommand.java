/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp.command;

import org.mule.extension.ftp.internal.ftp.connection.ClassicFtpFileSystem;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.command.CreateDirectoryCommand;

import org.apache.commons.net.ftp.FTPClient;

/**
 * A {@link ClassicFtpCommand} which implements the {@link CreateDirectoryCommand}
 *
 * @since 4.0
 */
public final class FtpCreateDirectoryCommand extends ClassicFtpCommand implements CreateDirectoryCommand {

  /**
   * {@inheritDoc}
   */
  public FtpCreateDirectoryCommand(ClassicFtpFileSystem fileSystem, FTPClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createDirectory(FileConnectorConfig config, String directoryPath) {
    super.createDirectory(config, directoryPath);
  }
}
