/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.ftp.connection;

import org.mule.extension.ftp.internal.ConnectionSettings;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

public final class FtpConnectionSettings extends ConnectionSettings {

  /**
   * The port number of the FTP server to connect
   */
  @Parameter
  @Optional(defaultValue = "21")
  @Placement(order = 2)
  private int port = 21;

  /**
   * Username for the FTP Server. Required if the server is authenticated.
   */
  @Parameter
  @Optional
  @Placement(order = 3)
  private String username;

  /**
   * Password for the FTP Server. Required if the server is authenticated.
   */
  @Parameter
  @Password
  @Optional
  @Placement(order = 4)
  private String password;

  public int getPort() {
    return port;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }
}
