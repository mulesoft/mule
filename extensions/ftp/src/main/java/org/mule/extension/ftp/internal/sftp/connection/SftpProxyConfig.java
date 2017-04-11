/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.sftp.connection;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Password;

/**
 * A Proxy configuration for the SFTP connector.
 *
 * @since 3.9
 */
public class SftpProxyConfig {

  public enum Protocol {
    HTTP, SOCKS4, SOCKS5
  };

  @Parameter
  private String host;

  @Parameter
  private int port;

  @Parameter
  @Optional
  private String username;

  @Parameter
  @Optional
  @Password
  private String password;

  @Parameter
  private Protocol protocol;

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public Protocol getProtocol() {
    return protocol;
  }

}
