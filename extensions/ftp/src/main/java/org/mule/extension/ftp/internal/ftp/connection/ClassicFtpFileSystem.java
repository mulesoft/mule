/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp.connection;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.extension.ftp.internal.FtpConnector.FTP_PROTOCOL;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.extension.ftp.api.ftp.ClassicFtpFileAttributes;
import org.mule.extension.ftp.api.ftp.FtpTransferMode;
import org.mule.extension.ftp.internal.ftp.command.FtpCopyCommand;
import org.mule.extension.ftp.internal.ftp.command.FtpCreateDirectoryCommand;
import org.mule.extension.ftp.internal.ftp.command.FtpDeleteCommand;
import org.mule.extension.ftp.internal.ftp.command.FtpListCommand;
import org.mule.extension.ftp.internal.ftp.command.FtpMoveCommand;
import org.mule.extension.ftp.internal.ftp.command.FtpReadCommand;
import org.mule.extension.ftp.internal.ftp.command.FtpRenameCommand;
import org.mule.extension.ftp.internal.ftp.command.FtpWriteCommand;
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link FtpFileSystem} for files residing on a FTP server
 *
 * @since 4.0
 */
public final class ClassicFtpFileSystem extends AbstractFileSystem implements FtpFileSystem {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClassicFtpFileSystem.class);

  private final MuleContext muleContext;
  private final FTPClient client;
  private final CopyCommand copyCommand;
  private final CreateDirectoryCommand createDirectoryCommand;
  private final DeleteCommand deleteCommand;
  private final ListCommand listCommand;
  private final MoveCommand moveCommand;
  private final ReadCommand readCommand;
  private final RenameCommand renameCommand;
  private final WriteCommand writeCommand;


  /**
   * Creates a new instance
   *
   * @param client a ready to use {@link FTPClient}
   */
  ClassicFtpFileSystem(FTPClient client, MuleContext muleContext) {
    this.client = client;
    this.muleContext = muleContext;

    copyCommand = new FtpCopyCommand(this, client);
    createDirectoryCommand = new FtpCreateDirectoryCommand(this, client);
    deleteCommand = new FtpDeleteCommand(this, client);
    listCommand = new FtpListCommand(this, client);
    moveCommand = new FtpMoveCommand(this, client);
    readCommand = new FtpReadCommand(this, client);
    renameCommand = new FtpRenameCommand(this, client);
    writeCommand = new FtpWriteCommand(this, client, muleContext);
  }

  /**
   * Severs the connection by invoking {@link FTPClient#logout()} and {@link FTPClient#disconnect()} on the provided
   * {@link #client}.
   * <p>
   * Notice that {@link FTPClient#disconnect()} will be invoked even if {@link FTPClient#logout()} fails. This method will never
   * throw exception. Any errors will be logged.
   */
  @Override
  public void disconnect() {
    try {
      client.logout();
    } catch (FTPConnectionClosedException e) {
      // this is valid and expected if the server closes the connection prematurely as a result of the logout... ignore
    } catch (Exception e) {
      LOGGER.warn("Exception found trying to logout from ftp at " + toURL(null), e);
    } finally {
      try {
        client.disconnect();
      } catch (Exception e) {
        LOGGER.warn("Exception found trying to disconnect from ftp at " + toURL(null), e);
      }
    }
  }

  /**
   * Validates the connection by sending a {@code NoOp} command
   *
   * @return a {@link ConnectionValidationResult}
   */
  @Override
  public ConnectionValidationResult validateConnection() {
    try {
      if (client.sendNoOp()) {
        return ConnectionValidationResult.success();
      } else {
        return ConnectionValidationResult.failure("NoOp did not complete", ConnectionExceptionCode.UNKNOWN, null);
      }
    } catch (IOException e) {
      return ConnectionValidationResult.failure("Found exception trying to perform validation", ConnectionExceptionCode.UNKNOWN,
                                                e);
    }
  }

  /**
   * Sets the transfer mode on the {@link #client}
   *
   * @param mode a {@link FtpTransferMode}
   */
  public void setTransferMode(FtpTransferMode mode) {
    try {
      if (!client.setFileType(mode.getCode())) {
        throw new IOException(String.format("Failed to set %s transfer type. FTP reply code is: ", mode.getDescription(),
                                            client.getReplyCode()));
      }
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage(String.format(
                                                                       "Found exception trying to change transfer mode to %s. FTP reply code is: ",
                                                                       mode.getClass(), client.getReplyCode())));
    }
  }

  /**
   * Sets the data timeout property on the underlying {@link #client}
   *
   * @param timeout a timeout scalar
   * @param timeUnit a {@link TimeUnit} which qualifies the {@code timeout}
   */
  public void setResponseTimeout(Integer timeout, TimeUnit timeUnit) {
    client.setDataTimeout(new Long(timeUnit.toMillis(timeout)).intValue());
  }

  /**
   * If {@code passive} is {@code true} then the {@link #client} is set on passive mode. Otherwise is set on active mode.
   *
   * @param passive whether to go passive mode or not
   */
  public void setPassiveMode(boolean passive) {
    if (passive) {
      LOGGER.debug("Entering FTP passive mode");
      client.enterLocalPassiveMode();
    } else {
      LOGGER.debug("Entering FTP active mode");
      client.enterLocalActiveMode();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream retrieveFileContent(FileAttributes filePayload) {
    try {
      InputStream inputStream = client.retrieveFileStream(filePayload.getPath());
      if (inputStream == null) {
        throw new FileNotFoundException(String.format("Could not retrieve content of file '%s' because it doesn't exists",
                                                      filePayload.getPath()));
      }

      return inputStream;
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage(format("Exception was found trying to retrieve the contents of file '%s'. Ftp reply code: %d ",
                                                                filePayload.getPath(), client.getReplyCode())),
                                     e);
    }
  }

  /**
   * Awaits for the underlying {@link #client} to complete any pending commands. This is necessary for certain operations such as
   * write. Using the {@link #client} before tnhat can result in unexpected behavior
   */
  public void awaitCommandCompletion() {
    try {
      if (!client.completePendingCommand()) {
        throw new IllegalStateException("Pending command did not complete");
      }
    } catch (IOException e) {
      throw new MuleRuntimeException(createStaticMessage("Failed to complete pending command. Ftp reply code: "
          + client.getReplyCode()), e);
    }
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
      return new URL(FTP_PROTOCOL, client.getRemoteAddress().toString(), client.getRemotePort(),
                     path != null ? path.toString() : EMPTY);
    } catch (MalformedURLException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not get URL for FTP server"), e);
    }
  }

  /**
   * Changes the {@link #client}'s current working directory to the {@code config}'s {@link FileConnectorConfig#getWorkingDir()}
   */
  @Override
  public void changeToBaseDir(FileConnectorConfig config) {
    if (config.getWorkingDir() != null) {
      try {
        client.changeWorkingDirectory(Paths.get(config.getWorkingDir()).toString());
      } catch (IOException e) {
        throw new MuleRuntimeException(createStaticMessage(format("Failed to perform CWD to the base directory '%s'",
                                                                  config.getWorkingDir())),
                                       e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ReadCommand getReadCommand() {
    return readCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ListCommand getListCommand() {
    return listCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected WriteCommand getWriteCommand() {
    return writeCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected CopyCommand getCopyCommand() {
    return copyCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected MoveCommand getMoveCommand() {
    return moveCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected DeleteCommand getDeleteCommand() {
    return deleteCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected RenameCommand getRenameCommand() {
    return renameCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected CreateDirectoryCommand getCreateDirectoryCommand() {
    return createDirectoryCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends FileAttributes> getAttributesType() {
    return ClassicFtpFileAttributes.class;
  }
}
