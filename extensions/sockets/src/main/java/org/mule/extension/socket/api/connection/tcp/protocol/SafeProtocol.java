/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.connection.tcp.protocol;

import org.mule.extension.socket.api.socket.tcp.TcpProtocol;
import org.mule.runtime.core.api.serialization.DefaultObjectSerializer;
import org.mule.runtime.core.api.serialization.ObjectSerializer;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;

/**
 * This protocol is an application level {@link TcpProtocol} and precedes every message with a cookie. It should probably not be
 * used in production. Should probably change to {@link LengthProtocol}. Both sender and receiver must use the same protocol.
 *
 * @since 4.0
 */
public class SafeProtocol extends AbstractByteProtocol {

  public static final String COOKIE = "You are using SafeProtocol";

  private final TcpProtocol cookieProtocol = new LengthProtocol(COOKIE.length());
  private TcpProtocol delegate;

  /**
   * Indicates the maximum length of the message
   */
  @Parameter
  @Optional(defaultValue = "-1")
  private int maxMessageLeght = NO_MAX_LENGTH;

  /**
   * Indicates if the data to transfer is just the Payload or the entire Mule Message
   */
  @Parameter
  @Optional(defaultValue = "true")
  private boolean payloadOnly = true;

  public SafeProtocol() {
    super(false);
    LengthProtocol protocol = new LengthProtocol();
    protocol.setPayloadOnly(payloadOnly);
    delegate = protocol;
  }

  /**
   * Reads the actual data only after assuring that the cookie was preceding the message.
   *
   * @param inputStream
   * @return {@code null} if the cookie could not be successfully received.
   * @throws IOException
   */
  public InputStream read(InputStream inputStream) throws IOException {
    if (assertSiblingSafe(inputStream)) {
      InputStream result = delegate.read(inputStream);
      if (null == result) {
        // EOF after cookie but before data
        helpUser();
      }
      return result;
    } else {
      throw new IOException("Safe protocol failed while asserting message prefix");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(OutputStream os, Object data, String encoding) throws IOException {
    assureSibling(os, encoding);
    delegate.write(os, data, encoding);
  }

  /**
   * Writes COOKIE message into #{code outputStream}. It should be invoked before sending the actual data.
   *
   * @param outputStream
   * @throws IOException
   */
  private void assureSibling(OutputStream outputStream, String encoding) throws IOException {
    cookieProtocol.write(outputStream, COOKIE, encoding);
  }

  /**
   * Reads from #{code inputStream} and checks that the COOKIE message was received successfully.
   *
   * @param inputStream Stream to read data from
   * @return true if further data are available; false if EOF
   * @throws IOException
   */
  private boolean assertSiblingSafe(InputStream inputStream) throws IOException {
    Object cookie = null;
    try {
      cookie = cookieProtocol.read(inputStream);
    } catch (Exception e) {
      helpUser(e);
    }
    if (null != cookie) {
      String parsedCookie = IOUtils.toString((InputStream) cookie);
      if (parsedCookie.length() != COOKIE.length() || !COOKIE.equals(parsedCookie)) {
        helpUser();
      } else {
        return true;
      }
    }
    return false; // eof
  }

  private void helpUser() throws IOException {
    throw new IOException("You are not using a consistent protocol on your TCP transport. "
        + "Please read the documentation for the TCP transport, " + "paying particular attention to the protocol parameter.");
  }

  private void helpUser(Exception e) throws IOException {
    throw (IOException) new IOException("An error occurred while verifying your connection.  "
        + "You may not be using a consistent protocol on your TCP transport. "
        + "Please read the documentation for the TCP transport, " + "paying particular attention to the protocol parameter.")
            .initCause(e);
  }

  @Inject
  @DefaultObjectSerializer
  public void setObjectSerializer(ObjectSerializer objectSerializer) {
    propagateObjectSerializerIfNecessary(delegate, objectSerializer);
    propagateObjectSerializerIfNecessary(cookieProtocol, objectSerializer);
  }

  private void propagateObjectSerializerIfNecessary(TcpProtocol protocol, ObjectSerializer objectSerializer) {
    if (protocol instanceof AbstractByteProtocol) {
      ((AbstractByteProtocol) protocol).setObjectSerializer(objectSerializer);
    }
  }

}
