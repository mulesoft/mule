/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.connection.tcp.protocol;

import org.mule.extension.socket.api.socket.tcp.TcpProtocol;
import org.mule.runtime.extension.api.annotation.Alias;

/**
 * This protocol is an application level {@link TcpProtocol} that does nothing. Reading is terminated by the stream being closed
 * by the client.
 * <p>
 *
 * @since 4.0
 */
@Alias("eof-protocol")
public class EOFProtocol extends DirectProtocol {

  /**
   * Repeat until EOF
   *
   * @param len Amount transferred last call (-1 on EOF or socket error)
   * @param available Amount available
   * @return true if the transfer should continue
   */
  @Override
  protected boolean isRepeat(int len, int available) {
    return true;
  }

}
