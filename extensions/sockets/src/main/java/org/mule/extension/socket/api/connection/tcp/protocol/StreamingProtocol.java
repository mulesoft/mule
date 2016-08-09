/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.connection.tcp.protocol;

import static org.mule.extension.socket.internal.SocketUtils.getByteArray;
import static org.mule.runtime.core.util.IOUtils.copyLarge;

import org.mule.extension.socket.api.SocketOperations;
import org.mule.extension.socket.api.config.RequesterConfig;
import org.mule.extension.socket.api.connection.RequesterConnection;
import org.mule.extension.socket.api.socket.tcp.TcpProtocol;
import org.mule.extension.socket.internal.TcpInputStream;
import org.mule.runtime.api.message.MuleMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This protocol is an application level {@link TcpProtocol} that wraps an {@link InputStream} and does not consume it. This
 * allows the {@link SocketOperations#send(RequesterConnection, RequesterConfig, Object, String, String, MuleMessage)} to return a
 * {@link MuleMessage} with the original {@link InputStream} as payload.
 *
 * @since 4.0
 */
public class StreamingProtocol extends EOFProtocol {

  public StreamingProtocol() {
    super();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream read(InputStream is) throws IOException {
    if (is instanceof TcpInputStream) {
      ((TcpInputStream) is).setStreaming(true);
    }

    return is;
  }


  @Override
  public void write(OutputStream os, Object data, String encoding) throws IOException {
    if (data instanceof InputStream) {
      InputStream is = (InputStream) data;
      copyLarge(is, os);
      os.flush();
      os.close();
      is.close();
    } else {
      this.writeByteArray(os, getByteArray(data, false, true, encoding, objectSerializer));
    }
  }
}


