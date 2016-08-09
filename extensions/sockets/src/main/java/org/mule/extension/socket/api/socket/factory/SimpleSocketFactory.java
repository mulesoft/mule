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
 * Interface for {@link Socket} factories
 * 
 * @since 4.0
 */
public interface SimpleSocketFactory {

  /**
   * Creates a {@link Socket}
   * 
   * @return a new instance of a {@link Socket} implementation
   * @throws IOException
   */
  Socket createSocket() throws IOException;
}
