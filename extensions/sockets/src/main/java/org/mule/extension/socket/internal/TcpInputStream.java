/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.internal;

import org.mule.extension.socket.api.socket.tcp.TcpProtocol;
import org.mule.extension.socket.api.worker.TcpWorker;

import java.io.InputStream;

import org.apache.commons.io.input.ProxyInputStream;

/**
 * Used in {@link TcpWorker} as the input parameter for the read() method on the {@link TcpProtocol} interface. If you wish to
 * simply use the InputStream as the message payload that you're reading in, you just call
 * {@link TcpInputStream#setStreaming(boolean)} so that Mule knows to stop listening for more messages on that stream.
 *
 * Also, the if streaming is activated, the {@link TcpWorker} using this stream will wait until the streaming has been completely
 * sent before closing the {@link InputStream}.
 */
public class TcpInputStream extends ProxyInputStream {

  private boolean streaming = false;

  public TcpInputStream(InputStream inputStream) {
    super(inputStream);
  }

  public boolean isStreaming() {
    return streaming;
  }

  public void setStreaming(boolean streaming) {
    this.streaming = streaming;
  }
}


