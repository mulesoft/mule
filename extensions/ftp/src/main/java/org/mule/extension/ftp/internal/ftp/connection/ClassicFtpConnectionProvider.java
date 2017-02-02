/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp.connection;

import static org.mule.extension.file.common.api.exceptions.FileError.CANNOT_REACH;
import static org.mule.extension.file.common.api.exceptions.FileError.CONNECTION_TIMEOUT;
import static org.mule.extension.file.common.api.exceptions.FileError.INVALID_CREDENTIALS;
import static org.mule.extension.file.common.api.exceptions.FileError.SERVICE_NOT_AVAILABLE;
import static org.mule.extension.file.common.api.exceptions.FileError.UNKNOWN_HOST;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.mule.extension.file.common.api.exceptions.FileError;
import org.mule.extension.ftp.api.FTPConnectionException;
import org.mule.extension.ftp.api.ftp.FtpTransferMode;
import org.mule.extension.ftp.internal.AbstractFtpConnectionProvider;
import org.mule.extension.ftp.internal.FtpConnector;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * An {@link AbstractFtpConnectionProvider} which provides instances of {@link ClassicFtpFileSystem} from instances of
 * {@link FtpConnector}
 *
 * @since 4.0
 */
@DisplayName("FTP Connection")
@Summary("Connection to connect against an FTP server")
public class ClassicFtpConnectionProvider extends AbstractFtpConnectionProvider<ClassicFtpFileSystem> {

  private static final String COULD_NOT_ESTABLISH_FTP_CONNECTION = "Could not establish FTP connection";

  @ParameterGroup(name = CONNECTION)
  private FtpConnectionSettings connectionSettings;

  /**
   * The transfer mode to be used. Currently {@code BINARY} and {@code ASCII} are supported.
   * <p>
   * Defaults to {@code BINARY}
   */
  @Parameter
  @Optional(defaultValue = "BINARY")
  @Summary("Transfer mode to be used")
  private FtpTransferMode transferMode;

  /**
   * Whether to use passive mode. Set to {@code false} to switch to active mode.
   * <p>
   * Defaults to {@code true}.
   */
  @Parameter
  @Optional(defaultValue = "true")
  @Summary("Whether to use passive mode. Set to \"false\" to switch to active mode")
  private boolean passive = true;

  /**
   * Creates and returns a new instance of {@link ClassicFtpFileSystem}
   *
   * @return a {@link ClassicFtpFileSystem}
   */
  @Override
  public ClassicFtpFileSystem connect() throws ConnectionException {
    return new ClassicFtpFileSystem(setupClient(), getWorkingDir(), muleContext);
  }

  private FTPClient setupClient() throws ConnectionException {
    FTPClient client = createClient();
    if (getConnectionTimeout() != null && getConnectionTimeoutUnit() != null) {
      client.setConnectTimeout(new Long(getConnectionTimeoutUnit().toMillis(getConnectionTimeout())).intValue());
    }

    try {
      client.connect(connectionSettings.getHost(), connectionSettings.getPort());
      if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
        throw handleClientReplyCode("FTP Connect failed", client.getReplyCode());
      }
      if (!client.login(connectionSettings.getUsername(), connectionSettings.getPassword())) {
        throw handleClientReplyCode("FTP login failed", client.getReplyCode());
      }
    } catch (SocketTimeoutException e) {
      throw new FTPConnectionException(COULD_NOT_ESTABLISH_FTP_CONNECTION, e, CONNECTION_TIMEOUT);
    } catch (ConnectException e) {
      throw new FTPConnectionException(COULD_NOT_ESTABLISH_FTP_CONNECTION, e, CANNOT_REACH);
    } catch (UnknownHostException e) {
      throw new FTPConnectionException(COULD_NOT_ESTABLISH_FTP_CONNECTION, e, UNKNOWN_HOST);
    } catch (Exception e) {
      throw client.getReplyCode() != 0
          ? handleClientReplyCode(COULD_NOT_ESTABLISH_FTP_CONNECTION, client.getReplyCode())
          : new ConnectionException(COULD_NOT_ESTABLISH_FTP_CONNECTION, e);
    }

    return client;
  }

  protected FTPClient createClient() {
    return new FTPClient();
  }

  @Override
  public void onBorrow(ClassicFtpFileSystem connection) {
    connection.setTransferMode(transferMode);
    connection.setResponseTimeout(getResponseTimeout(), getResponseTimeoutUnit());
    connection.setPassiveMode(passive);
  }

  /**
   * Handles a {@link FTPClient} reply code and returns a {@link FTPConnectionException} specifying the correspondent
   * {@link FileError} indicating the cause of the failure.
   *
   * @param errorMessage Message to throw
   * @param replyCode FTP Client reply code
   * @return A {@link FTPConnectionException} specifying the error cause with a {@link FileError}
   */
  private ConnectionException handleClientReplyCode(String errorMessage, int replyCode) {
    switch (replyCode) {
      case 501:
      case 530:
        return new FTPConnectionException(errorMessage + " : " + replyCode + "- User cannot log in", INVALID_CREDENTIALS);
      case 421:
        return new FTPConnectionException(errorMessage, SERVICE_NOT_AVAILABLE);
    }
    return new FTPConnectionException(errorMessage + " : " + replyCode);
  }
}
