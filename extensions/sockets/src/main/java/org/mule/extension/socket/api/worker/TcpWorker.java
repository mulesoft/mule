/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.worker;

import static java.lang.String.format;

import org.mule.extension.socket.api.ImmutableSocketAttributes;
import org.mule.extension.socket.api.SocketAttributes;
import org.mule.extension.socket.api.connection.tcp.TcpListenerConnection;
import org.mule.extension.socket.api.socket.tcp.TcpProtocol;
import org.mule.extension.socket.internal.TcpInputStream;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Only one worker will be created per each new TCP connection accepted by the
 * {@link TcpListenerConnection#listen(SourceCallback)}, This class is responsible for reading from that connection
 * is closed by the sender, or {@link Source} is stopped.
 *
 * @since 4.0
 */
public final class TcpWorker extends SocketWorker {

  private static final Logger LOGGER = LoggerFactory.getLogger(TcpWorker.class);
  private final Socket socket;
  private final TcpInputStream dataIn;
  private final OutputStream dataOut;
  private final InputStream underlyingIn;
  private final TcpProtocol protocol;
  private Object notify = new Object();
  private boolean dataInWorkFinished = false;
  private AtomicBoolean moreMessages = new AtomicBoolean(true); // can be set on completion's callback

  public TcpWorker(Socket socket, TcpProtocol protocol, SourceCallback<InputStream, SocketAttributes> callback)
      throws IOException {
    super(callback);
    this.socket = socket;
    this.protocol = protocol;

    underlyingIn = new BufferedInputStream(socket.getInputStream());
    dataOut = new BufferedOutputStream(socket.getOutputStream());
    dataIn = new TcpInputStream(underlyingIn) {

      @Override
      public void close() throws IOException {
        // Don't actually close the stream, we just want to know if the
        // we want to stop receiving messages on this sockete.
        // The Protocol is responsible for closing this.
        dataInWorkFinished = true;
        moreMessages.set(false);

        synchronized (notify) {
          notify.notifyAll();
        }
      }
    };
  }

  private void waitForStreams() {
    // The Message with the InputStream as a payload can be dispatched
    // into a different thread, in which case we need to wait for it to
    // finish streaming
    if (!dataInWorkFinished) {
      synchronized (notify) {
        if (!dataInWorkFinished) {
          try {
            notify.wait();
          } catch (InterruptedException e) {
          }
        }
      }
    }
  }

  private InputStream getNextMessage() throws IOException {
    InputStream readMsg = null;
    try {
      readMsg = protocol.read(dataIn);
      if (dataIn.isStreaming()) {
        moreMessages.set(false);
      }

      return readMsg;
    } finally {
      if (readMsg == null) {
        dataIn.close();
      }
    }
  }

  protected void shutdownSocket() throws IOException {
    try {
      socket.shutdownOutput();
    } catch (UnsupportedOperationException e) {
      // Ignore, not supported by ssl sockets
    }
  }

  private boolean hasMoreMessages() {
    return !socket.isClosed() && !dataInWorkFinished && moreMessages.get();
  }

  @Override
  public void doRun() {
    while (hasMoreMessages()) {
      InputStream content;
      try {
        content = getNextMessage();
      } catch (IOException e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("An error occurred while reading from the TCP Worker connection", e);
        }

        moreMessages.set(false);
        break;
      }

      if (content == null) {
        moreMessages.set(false);
        break;
      }

      handle(content, new ImmutableSocketAttributes(socket));
    }
  }

  @Override
  public void onComplete(Object result) {
    try {
      protocol.write(dataOut, result, encoding);
      dataOut.flush();
    } catch (IOException e) {
      callback.onSourceException(new IOException(format("An error occurred while sending TCP response to address '%s'",
                                                        socket.getRemoteSocketAddress().toString(), e)));
    }
  }

  @Override
  public void onError(Throwable e) {
    LOGGER.error("TCP worker will not answer back due an exception was received", e);

    // end worker's execution
    moreMessages.set(false);
  }

  @Override
  public void dispose() {
    releaseSocket();
  }

  private void releaseSocket() {
    if (socket != null && !socket.isClosed()) {
      try {
        shutdownSocket();
      } catch (IOException e) {
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn("TCP Worker shutting down output stream failed", e);
        }
      } finally {
        try {
          socket.close();
        } catch (IOException e) {
          if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("TCP Worker socket close failed", e);
          }
        }
      }
    }
  }
}
