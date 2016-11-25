/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

/**
 * Base class for groups of FTP/SFTP connection parameters
 *
 * @since 1.0
 */
public abstract class BaseFtpConnectionSettings {

  /**
   * The FTP server host, such as www.mulesoft.com, localhost, or 192.168.0.1, etc
   */
  @Parameter
  @Placement(order = 1)
  private String host;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }
}
