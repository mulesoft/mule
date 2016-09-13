/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file.internal.command;

import org.mule.extension.file.api.LocalFileAttributes;
import org.mule.extension.file.internal.FileInputStream;
import org.mule.extension.file.internal.LocalFileSystem;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.runtime.operation.OperationResult;
import org.mule.extension.file.common.api.FileAttributes;
import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.command.ReadCommand;
import org.mule.extension.file.common.api.lock.NullPathLock;
import org.mule.extension.file.common.api.lock.PathLock;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A {@link LocalFileCommand} which implements the {@link ReadCommand} contract
 *
 * @since 4.0
 */
public final class LocalReadCommand extends LocalFileCommand implements ReadCommand {

  /**
   * {@inheritDoc}
   */
  public LocalReadCommand(LocalFileSystem fileSystem) {
    super(fileSystem);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OperationResult<InputStream, FileAttributes> read(FileConnectorConfig config, Message message, String filePath,
                                                           boolean lock) {
    Path path = resolveExistingPath(filePath);
    if (Files.isDirectory(path)) {
      throw cannotReadDirectoryException(path);
    }

    PathLock pathLock;
    if (lock) {
      pathLock = fileSystem.lock(path);
    } else {
      fileSystem.verifyNotLocked(path);
      pathLock = new NullPathLock();
    }

    InputStream payload = new FileInputStream(path, pathLock);
    FileAttributes fileAttributes = new LocalFileAttributes(path);
    MediaType fileMediaType =
        fileSystem.getFileMessageMediaType(message.getPayload().getDataType().getMediaType(), fileAttributes);
    return OperationResult.<InputStream, FileAttributes>builder().output(payload).mediaType(fileMediaType)
        .attributes(fileAttributes).build();
  }
}
