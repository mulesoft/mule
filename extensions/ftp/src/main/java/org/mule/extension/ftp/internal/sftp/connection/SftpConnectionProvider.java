/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.sftp.connection;

import static java.lang.String.format;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;
import com.jcraft.jsch.JSchException;
import org.mule.extension.file.common.api.exceptions.FileError;
import org.mule.extension.ftp.api.FTPConnectionException;
import org.mule.extension.ftp.api.sftp.SftpAuthenticationMethod;
import org.mule.extension.ftp.internal.AbstractFtpConnectionProvider;
import org.mule.extension.ftp.internal.FtpConnector;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

import com.google.common.base.Joiner;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * An {@link AbstractFtpConnectionProvider} which provides instances of {@link SftpFileSystem} from instances of
 * {@link FtpConnector}
 *
 * @since 4.0
 */
@Alias("sftp")
@DisplayName("SFTP Connection")
public class SftpConnectionProvider extends AbstractFtpConnectionProvider<SftpFileSystem> {

  private static final String AUTH_FAIL_MESSAGE = "Auth fail";
  private static final String SSH_DISCONNECTION_MESSAGE = "SSH_MSG_DISCONNECT";
  private static final String TIMEOUT = "timeout";

  @ParameterGroup(name = CONNECTION)
  private SftpConnectionSettings connectionSettings = new SftpConnectionSettings();

  /**
   * Set of authentication methods used by the SFTP client. Valid values are: gssapi-with-mic, publickey, keyboard-interactive and
   * password.
   */
  @Parameter
  @Optional
  private Set<SftpAuthenticationMethod> preferredAuthenticationMethods;

  /**
   * If provided, the client will validate the server's key against the one in the referenced file. If the server key doesn't
   * match the one in the file, the connection will be aborted.
   */
  @Parameter
  @Optional
  private String knownHostsFile;

  private SftpClientFactory clientFactory = new SftpClientFactory();

  @Override
  public SftpFileSystem connect() throws ConnectionException {
    SftpClient client = clientFactory.createInstance(connectionSettings.getHost(), connectionSettings.getPort());
    client.setConnectionTimeoutMillis(getConnectionTimeoutUnit().toMillis(getConnectionTimeout()));
    client.setPassword(connectionSettings.getPassword());
    client.setIdentity(connectionSettings.getIdentityFile(), connectionSettings.getPassphrase());
    if (!CollectionUtils.isEmpty(preferredAuthenticationMethods)) {
      client.setPreferredAuthenticationMethods(Joiner.on(",").join(preferredAuthenticationMethods));
    }
    client.setKnownHostsFile(knownHostsFile);
    try {
      client.login(connectionSettings.getUsername());
    } catch (JSchException e) {
      handleJSchException(e);
    } catch (Exception e) {
      throw new ConnectionException(e);
    }

    return new SftpFileSystem(client, getWorkingDir(), muleContext);
  }

  void setPort(int port) {
    connectionSettings.setPort(port);
  }

  void setHost(String host) {
    connectionSettings.setHost(host);
  }

  void setUsername(String username) {
    connectionSettings.setUsername(username);
  }

  void setPassword(String password) {
    connectionSettings.setPassword(password);
  }

  void setPassphrase(String passphrase) {
    connectionSettings.setPassphrase(passphrase);
  }

  void setIdentityFile(String identityFile) {
    connectionSettings.setIdentityFile(identityFile);
  }

  void setPreferredAuthenticationMethods(Set<SftpAuthenticationMethod> preferredAuthenticationMethods) {
    this.preferredAuthenticationMethods = preferredAuthenticationMethods;
  }

  void setKnownHostsFile(String knownHostsFile) {
    this.knownHostsFile = knownHostsFile;
  }

  void setClientFactory(SftpClientFactory clientFactory) {
    this.clientFactory = clientFactory;
  }

  /**
   * Handles a {@link JSchException}, introspects their cause or message to return a {@link ConnectionException}
   * indicating with a {@link FileError} the kind of failure.
   * @param e The exception to handle
   * @throws ConnectionException Indicating the kind of failure
   */
  private void handleJSchException(JSchException e) throws ConnectionException {
    String message = e.getMessage();
    if (message.equals(AUTH_FAIL_MESSAGE)) {
      throw new FTPConnectionException(format("Error during login to %s@%s", connectionSettings.getUsername(),
                                              connectionSettings.getHost()),
                                       e, FileError.INVALID_CREDENTIALS);
    }
    if (e.getMessage().startsWith(TIMEOUT)) {
      throw new FTPConnectionException(e, FileError.CONNECTION_TIMEOUT);
    }
    if (e.getMessage().startsWith(SSH_DISCONNECTION_MESSAGE)) {
      throw new FTPConnectionException("An error occurred connecting to the SFTP server: " + e.getMessage(), e,
                                       FileError.DISCONNECTED);
    }
    if (e.getCause() instanceof ConnectException) {
      throw new FTPConnectionException(e, FileError.CANNOT_REACH);
    }
    if (e.getCause() instanceof UnknownHostException) {
      throw new FTPConnectionException(e, FileError.UNKNOWN_HOST);
    }
    throw new ConnectionException(e);
  }
}
