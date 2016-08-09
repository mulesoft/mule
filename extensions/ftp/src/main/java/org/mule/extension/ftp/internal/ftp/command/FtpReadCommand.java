/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp.command;

import org.mule.extension.ftp.api.FtpFileAttributes;
import org.mule.extension.ftp.api.ftp.ClassicFtpFileAttributes;
import org.mule.extension.ftp.internal.FtpConnector;
import org.mule.extension.ftp.internal.ftp.ClassicFtpInputStream;
import org.mule.extension.ftp.internal.ftp.connection.ClassicFtpFileSystem;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.runtime.operation.OperationResult;
import org.mule.runtime.module.extension.file.api.FileAttributes;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.command.ReadCommand;
import org.mule.runtime.module.extension.file.api.lock.NullPathLock;
import org.mule.runtime.module.extension.file.api.lock.PathLock;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.net.ftp.FTPClient;

/**
 * A {@link ClassicFtpCommand} which implements the {@link FtpReadCommand}
 *
 * @since 4.0
 */
public final class FtpReadCommand extends ClassicFtpCommand implements ReadCommand {

  /**
   * {@inheritDoc}
   */
  public FtpReadCommand(ClassicFtpFileSystem fileSystem, FTPClient client) {
    super(fileSystem, client);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OperationResult<InputStream, FileAttributes> read(FileConnectorConfig config, MuleMessage message, String filePath,
                                                           boolean lock) {
    FtpFileAttributes attributes = getExistingFile(config, filePath);
    if (attributes.isDirectory()) {
      throw cannotReadDirectoryException(Paths.get(attributes.getPath()));
    }

    try {
      attributes = new ClassicFtpFileAttributes(resolvePath(config, filePath), client.listFiles(filePath)[0]);
    } catch (Exception e) {
      throw exception("Found exception while trying to read path " + filePath, e);
    }

    Path path = Paths.get(attributes.getPath());

    PathLock pathLock;
    if (lock) {
      pathLock = fileSystem.lock(path);
    } else {
      fileSystem.verifyNotLocked(path);
      pathLock = new NullPathLock();
    }

    try {
      InputStream payload = ClassicFtpInputStream.newInstance((FtpConnector) config, attributes, pathLock);
      MediaType mediaType = fileSystem.getFileMessageMediaType(message.getDataType().getMediaType(), attributes);
      return OperationResult.<InputStream, FileAttributes>builder().output(payload).mediaType(mediaType).attributes(attributes)
          .build();
    } catch (ConnectionException e) {
      throw exception("Could not obtain connection to fetch file " + path, e);
    }
  }
}
