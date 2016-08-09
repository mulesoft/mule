/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.sftp.command;

import org.mule.extension.ftp.api.FtpFileAttributes;
import org.mule.extension.ftp.internal.sftp.connection.SftpFileSystem;
import org.mule.extension.ftp.internal.ftp.command.FtpCommand;
import org.mule.extension.ftp.api.sftp.SftpFileAttributes;
import org.mule.extension.ftp.internal.sftp.connection.SftpClient;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Stack;

/**
 * Base class for {@link FtpCommand} implementations that target a SFTP server
 *
 * @since 4.0
 */
public abstract class SftpCommand extends FtpCommand<SftpFileSystem> {

  protected SftpClient client;

  /**
   * Creates a new instance
   *
   * @param fileSystem a {@link SftpFileSystem} used as the connection object
   * @param client a {@link SftpClient}
   */
  public SftpCommand(SftpFileSystem fileSystem, SftpClient client) {
    super(fileSystem);
    this.client = client;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected FtpFileAttributes getFile(FileConnectorConfig config, String filePath, boolean requireExistence) {
    Path path = resolvePath(config, filePath);
    SftpFileAttributes attributes;
    try {
      attributes = client.getAttributes(path);
    } catch (Exception e) {
      throw exception("Found exception trying to obtain path " + path, e);
    }

    if (attributes != null) {
      return attributes;
    } else {
      if (requireExistence) {
        throw pathNotFoundException(path);
      } else {
        return null;
      }
    }
  }

  /**
   * Creates the directory pointed by {@code directoryPath} also creating any missing parent directories
   *
   * @param directoryPath the {@link Path} to the directory you want to create
   */
  @Override
  protected void doMkDirs(FileConnectorConfig config, Path directoryPath) {
    Stack<Path> fragments = new Stack<>();
    for (int i = directoryPath.getNameCount(); i >= 0; i--) {
      Path subPath = Paths.get("/").resolve(directoryPath.subpath(0, i));
      if (exists(config, subPath)) {
        break;
      }
      fragments.push(subPath);
    }

    while (!fragments.isEmpty()) {
      Path fragment = fragments.pop();
      client.mkdir(fragment.toString());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean tryChangeWorkingDirectory(String path) {
    try {
      client.changeWorkingDirectory(path);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doRename(String filePath, String newName) throws Exception {
    client.rename(filePath, newName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getCurrentWorkingDirectory() {
    try {
      return client.getWorkingDirectory();
    } catch (Exception e) {
      throw exception("Failed to determine current working directory");
    }
  }
}
