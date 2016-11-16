/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.sftp.connection;

import org.mule.extension.ftp.internal.ConnectionSettings;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

public final class SftpConnectionSettings extends ConnectionSettings {

  /**
   * The port number of the SFTP server to connect on
   */
  @Parameter
  @Optional(defaultValue = "22")
  @Placement(order = 2)
  private int port = 22;

  /**
   * Username for the FTP Server. Required if the server is authenticated.
   */
  @Parameter
  @Optional
  @Placement(order = 3)
  protected String username;

  /**
   * Password for the FTP Server. Required if the server is authenticated.
   */
  @Parameter
  @Optional
  @Password
  @Placement(order = 4)
  private String password;

  /**
   * The passphrase (password) for the identityFile if required. Notice that this parameter is ignored if {@link #identityFile} is
   * not provided
   */
  @Parameter
  @Optional
  @Password
  @Placement(order = 6)
  @Summary("The passphrase (password) for the identityFile, if configured")
  private String passphrase;

  /**
   * An identityFile location for a PKI private key.
   */
  @Parameter
  @Optional
  @Placement(order = 5)
  private String identityFile;

  public int getPort() {
    return port;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getPassphrase() {
    return passphrase;
  }

  public String getIdentityFile() {
    return identityFile;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setPassphrase(String passphrase) {
    this.passphrase = passphrase;
  }

  public void setIdentityFile(String identityFile) {
    this.identityFile = identityFile;
  }
}
