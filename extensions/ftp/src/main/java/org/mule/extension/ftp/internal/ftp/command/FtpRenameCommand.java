/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp.command;

import org.mule.extension.ftp.internal.ftp.connection.ClassicFtpFileSystem;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.command.RenameCommand;

import org.apache.commons.net.ftp.FTPClient;

/**
 * A {@link ClassicFtpCommand} which implements the {@link RenameCommand}
 *
 * @since 4.0
 */
public final class FtpRenameCommand extends ClassicFtpCommand implements RenameCommand {

  /**
   * {@inheritDoc}
   */
  public FtpRenameCommand(ClassicFtpFileSystem fileSystem, FTPClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void rename(FileConnectorConfig config, String filePath, String newName, boolean overwrite) {
    super.rename(config, filePath, newName, overwrite);
  }
}
