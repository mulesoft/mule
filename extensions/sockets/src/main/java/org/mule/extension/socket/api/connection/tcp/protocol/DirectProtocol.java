/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.connection.tcp.protocol;

import static org.mule.extension.socket.internal.SocketUtils.getByteArray;

import org.mule.extension.socket.api.socket.tcp.TcpProtocol;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This protocol is an application level {@link TcpProtocol} that does nothing. The socket reads until no more bytes are
 * (momentarily) available (previously the transfer buffer also had to be full on the previous read, which made stronger
 * requirements on the underlying network). On slow networks {@link EOFProtocol} and {@link LengthProtocol} may be more reliable.
 * <p>
 * <p>
 * Writing simply writes the data into the socket.
 * </p>
 *
 * @since 4.0
 */
public class DirectProtocol extends AbstractByteProtocol {

  protected static final int UNLIMITED = -1;

  private static final Log LOGGER = LogFactory.getLog(DirectProtocol.class);
  private static final int DEFAULT_BUFFER_SIZE = 8192;

  /**
   * Indicates if the data to transfer is just the Payload or the entire Mule Message
   */
  @Parameter
  @Optional(defaultValue = "true")
  private boolean payloadOnly = true;

  protected int bufferSize;

  public DirectProtocol() {
    this(STREAM_OK, DEFAULT_BUFFER_SIZE);
  }

  public DirectProtocol(boolean streamOk, int bufferSize) {
    super(streamOk);
    this.bufferSize = bufferSize;
  }

  public void setPayloadOnly(boolean payloadOnly) {
    this.payloadOnly = payloadOnly;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream read(InputStream socketIs) throws IOException {
    return nullIfEmptyArray(consume(socketIs, UNLIMITED));
  }

  protected byte[] consume(InputStream is, int limit) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(bufferSize);

    try {
      byte[] buffer = new byte[bufferSize];
      int len;
      int remain = remaining(limit, limit, 0);
      boolean repeat;
      do {
        len = copy(is, buffer, byteArrayOutputStream, remain);
        remain = remaining(limit, remain, len);
        repeat = EOF != len && remain > 0 && isRepeat(len, is.available());
      } while (repeat);
    } finally {
      byteArrayOutputStream.flush();
      byteArrayOutputStream.close();
    }

    return byteArrayOutputStream.toByteArray();
  }

  protected int remaining(int limit, int remain, int len) {
    if (UNLIMITED == limit) {
      return bufferSize;
    } else if (EOF != len) {
      return remain - len;
    } else {
      return remain;
    }
  }

  /**
   * Decide whether to repeat transfer. This implementation does so if more data are available. Note that previously, while
   * documented as such, there was also the additional requirement that the previous transfer completely used the transfer buffer.
   *
   * @param len Amount transferred last call (-1 on EOF or socket error)
   * @param available Amount available
   * @return true if the transfer should continue
   */
  protected boolean isRepeat(int len, int available) {
    return available > 0;
  }

  @Override
  public void write(OutputStream os, Object data, String encoding) throws IOException {
    this.writeByteArray(os, getByteArray(data, payloadOnly, streamOk, encoding, objectSerializer));
  }
}
