/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.sftp.connection;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.extension.ftp.internal.FtpConnector.FTP_PROTOCOL;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.extension.ftp.api.sftp.SftpFileAttributes;
import org.mule.extension.ftp.internal.ftp.connection.FtpFileSystem;
import org.mule.extension.ftp.internal.sftp.command.SftpCopyCommand;
import org.mule.extension.ftp.internal.sftp.command.SftpCreateDirectoryCommand;
import org.mule.extension.ftp.internal.sftp.command.SftpDeleteCommand;
import org.mule.extension.ftp.internal.sftp.command.SftpListCommand;
import org.mule.extension.ftp.internal.sftp.command.SftpMoveCommand;
import org.mule.extension.ftp.internal.sftp.command.SftpReadCommand;
import org.mule.extension.ftp.internal.sftp.command.SftpRenameCommand;
import org.mule.extension.ftp.internal.sftp.command.SftpWriteCommand;
import org.mule.runtime.api.connection.ConnectionExceptionCode;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.module.extension.file.api.AbstractFileSystem;
import org.mule.runtime.module.extension.file.api.FileAttributes;
import org.mule.runtime.module.extension.file.api.FileConnectorConfig;
import org.mule.runtime.module.extension.file.api.command.CopyCommand;
import org.mule.runtime.module.extension.file.api.command.CreateDirectoryCommand;
import org.mule.runtime.module.extension.file.api.command.DeleteCommand;
import org.mule.runtime.module.extension.file.api.command.ListCommand;
import org.mule.runtime.module.extension.file.api.command.MoveCommand;
import org.mule.runtime.module.extension.file.api.command.ReadCommand;
import org.mule.runtime.module.extension.file.api.command.RenameCommand;
import org.mule.runtime.module.extension.file.api.command.WriteCommand;
import org.mule.runtime.module.extension.file.api.lock.PathLock;
import org.mule.runtime.module.extension.file.api.lock.URLPathLock;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import org.apache.commons.net.ftp.FTPClient;

/**
 * Implementation of {@link FtpFileSystem} for files residing on a SFTP server
 *
 * @since 4.0
 */
public class SftpFileSystem extends AbstractFileSystem implements FtpFileSystem {

  private final MuleContext muleContext;
  protected final SftpClient client;
  protected final CopyCommand copyCommand;
  protected final CreateDirectoryCommand createDirectoryCommand;
  protected final DeleteCommand deleteCommand;
  protected final ListCommand listCommand;
  protected final MoveCommand moveCommand;
  protected final ReadCommand readCommand;
  protected final RenameCommand renameCommand;
  protected final WriteCommand writeCommand;


  /**
   * Creates a new instance
   *
   * @param client a ready to use {@link FTPClient}
   */
  public SftpFileSystem(SftpClient client, MuleContext muleContext) {
    this.client = client;
    this.muleContext = muleContext;

    copyCommand = new SftpCopyCommand(this, client);
    createDirectoryCommand = new SftpCreateDirectoryCommand(this, client);
    deleteCommand = new SftpDeleteCommand(this, client);
    listCommand = new SftpListCommand(this, client);
    moveCommand = new SftpMoveCommand(this, client);
    readCommand = new SftpReadCommand(this, client);
    renameCommand = new SftpRenameCommand(this, client);
    writeCommand = new SftpWriteCommand(this, client, muleContext);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void disconnect() {
    client.disconnect();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void changeToBaseDir(FileConnectorConfig config) {
    client.changeWorkingDirectory(config.getWorkingDir());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream retrieveFileContent(FileAttributes filePayload) {
    return client.getFileContent(filePayload.getPath());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionValidationResult validateConnection() {
    return client.isConnected() ? ConnectionValidationResult.success()
        : ConnectionValidationResult.failure("Connection is stale", ConnectionExceptionCode.UNKNOWN, null);
  }

  /**
   * {@inheritDoc}
   *
   * @return a {@link URLPathLock} based on the {@link #client}'s connection information
   */
  @Override
  protected PathLock createLock(Path path, Object... params) {
    return new URLPathLock(toURL(path), muleContext.getLockFactory());
  }

  private URL toURL(Path path) {
    try {
      return new URL(FTP_PROTOCOL, client.getHost(), client.getPort(), path != null ? path.toString() : EMPTY);
    } catch (MalformedURLException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not get URL for SFTP server"), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CopyCommand getCopyCommand() {
    return copyCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CreateDirectoryCommand getCreateDirectoryCommand() {
    return createDirectoryCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DeleteCommand getDeleteCommand() {
    return deleteCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ListCommand getListCommand() {
    return listCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MoveCommand getMoveCommand() {
    return moveCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ReadCommand getReadCommand() {
    return readCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RenameCommand getRenameCommand() {
    return renameCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WriteCommand getWriteCommand() {
    return writeCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends FileAttributes> getAttributesType() {
    return SftpFileAttributes.class;
  }
}
