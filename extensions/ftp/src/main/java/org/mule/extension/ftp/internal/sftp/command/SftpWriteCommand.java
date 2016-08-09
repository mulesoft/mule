/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.sftp.command;

import static java.lang.String.format;
import org.mule.extension.ftp.internal.sftp.connection.SftpFileSystem;
import org.mule.extension.ftp.internal.sftp.connection.SftpClient;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.extension.file.api.FileAttributes;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.FileContentWrapper;
import org.mule.runtime.module.extension.file.api.FileWriteMode;
import org.mule.runtime.module.extension.file.api.FileWriterVisitor;
import org.mule.runtime.module.extension.file.api.command.WriteCommand;

import java.io.OutputStream;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SftpCommand} which implements the {@link WriteCommand} contract
 *
 * @since 4.0
 */
public final class SftpWriteCommand extends SftpCommand implements WriteCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(SftpWriteCommand.class);

  private final MuleContext muleContext;

  /**
   * {@inheritDoc}
   */
  public SftpWriteCommand(SftpFileSystem fileSystem, SftpClient client, MuleContext muleContext) {
    super(fileSystem, client);
    this.muleContext = muleContext;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(FileConnectorConfig config, String filePath, Object content, FileWriteMode mode, MuleEvent event,
                    boolean lock, boolean createParentDirectory, String encoding) {
    Path path = resolvePath(config, filePath);
    FileAttributes file = getFile(config, filePath);

    if (file == null) {
      assureParentFolderExists(config, path, createParentDirectory);
    } else {
      if (mode == FileWriteMode.CREATE_NEW) {
        throw new IllegalArgumentException(String.format(
                                                         "Cannot write to path '%s' because it already exists and write mode '%s' was selected. "
                                                             + "Use a different write mode or point to a path which doesn't exists",
                                                         path, mode));
      }
    }

    try (OutputStream outputStream = getOutputStream(path, mode)) {
      new FileContentWrapper(content, event, muleContext).accept(new FileWriterVisitor(outputStream, event, encoding));
      LOGGER.debug("Successfully wrote to path {}", path.toString());
    } catch (Exception e) {
      throw exception(format("Exception was found writing to file '%s'", path), e);
    }
  }

  private OutputStream getOutputStream(Path path, FileWriteMode mode) {
    try {
      return client.getOutputStream(path.toString(), mode);
    } catch (Exception e) {
      throw exception(String.format("Could not open stream to write to path '%s' using mode '%s'", path, mode), e);
    }
  }
}
