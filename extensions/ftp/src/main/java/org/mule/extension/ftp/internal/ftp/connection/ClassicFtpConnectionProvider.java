/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp.connection;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.CONNECTION;
import org.mule.extension.ftp.api.ftp.FtpTransferMode;
import org.mule.extension.ftp.internal.AbstractFtpConnectionProvider;
import org.mule.extension.ftp.internal.FtpConnector;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
 * An {@link AbstractFtpConnectionProvider} which provides instances of {@link ClassicFtpFileSystem} from instances of
 * {@link FtpConnector}
 *
 * @since 4.0
 */
@DisplayName("FTP Connection")
@Summary("Connection to connect against an FTP server")
public class ClassicFtpConnectionProvider extends AbstractFtpConnectionProvider<ClassicFtpFileSystem> {

  /**
   * The port number of the FTP server to connect
   */
  @Parameter
  @Optional(defaultValue = "21")
  @Placement(group = CONNECTION, order = 2)
  private int port = 21;

  /**
   * Username for the FTP Server. Required if the server is authenticated.
   */
  @Parameter
  @Optional
  @Placement(group = CONNECTION, order = 3)
  private String username;

  /**
   * Password for the FTP Server. Required if the server is authenticated.
   */
  @Parameter
  @Password
  @Optional
  @Placement(group = CONNECTION, order = 4)
  private String password;

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
    return new ClassicFtpFileSystem(setupClient(), muleContext);
  }

  private FTPClient setupClient() throws ConnectionException {
    FTPClient client = createClient();
    if (getConnectionTimeout() != null && getConnectionTimeoutUnit() != null) {
      client.setConnectTimeout(new Long(getConnectionTimeoutUnit().toMillis(getConnectionTimeout())).intValue());
    }

    try {
      client.connect(getHost(), port);
      if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
        throw new IOException("Ftp connect failed: " + client.getReplyCode());
      }
      if (!client.login(username, password)) {
        throw new IOException("Ftp login failed: " + client.getReplyCode());
      }
    } catch (Exception e) {
      throw new ConnectionException("Could not establish FTP connection", e);
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
}
