/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp.protocols;

import org.mule.compatibility.transport.tcp.TcpProtocol;
import org.mule.runtime.core.util.ClassUtils;

import java.io.IOException;
import java.io.OutputStream;

public class ProtocolStream extends OutputStream {

  private boolean streamOk;
  private TcpProtocol protocol;
  private OutputStream os;

  public ProtocolStream(TcpProtocol protocol, boolean streamOk, OutputStream os) {
    this.protocol = protocol;
    this.streamOk = streamOk;
    this.os = os;
  }

  private void assertStreamOk() {
    if (!streamOk) {
      throw new IllegalArgumentException("TCP protocol " + ClassUtils.getSimpleName(protocol.getClass())
          + " does not support streaming output");
    }
  }

  @Override
  public void write(byte b[]) throws IOException {
    assertStreamOk();
    protocol.write(os, b);
  }

  @Override
  public void write(byte b[], int off, int len) throws IOException {
    assertStreamOk();
    byte[] buffer = new byte[len];
    System.arraycopy(b, off, buffer, 0, len);
    protocol.write(os, buffer);
  }

  @Override
  public void flush() throws IOException {
    assertStreamOk();
    os.flush();
  }

  @Override
  public void write(int b) throws IOException {
    write(new byte[] {(byte) b});
  }

  @Override
  public void close() throws IOException {
    assertStreamOk();
    os.close();
  }

}
