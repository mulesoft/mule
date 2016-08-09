/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal.sftp.connection;

import com.jcraft.jsch.JSch;

/**
 * Creates instances of {@link SftpClient}
 *
 * @since 4.0
 */
public class SftpClientFactory {

  /**
   * Creates a new instance which will connect to the given {@code host} and {@code port}
   *
   * @param host the host address
   * @param port the remote connection port
   * @return a {@link SftpClient}
   */
  public SftpClient createInstance(String host, int port) {
    return new SftpClient(host, port, JSch::new);
  }
}
