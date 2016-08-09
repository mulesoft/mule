/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.socket.factory;

import java.io.IOException;
import java.net.Socket;

/**
 * Concrete implementation for {@link SimpleSocketFactory}. It provides TCP {@link Socket}.
 *
 * @since 4.0
 */
public class TcpSocketFactory implements SimpleSocketFactory {

  /**
   * {@inheritDoc}
   */
  @Override
  public Socket createSocket() throws IOException {
    return new Socket();
  }
}
