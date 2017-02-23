/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api;

import org.mule.extension.socket.api.client.SocketClient;
import org.mule.extension.socket.api.config.RequesterConfig;
import org.mule.extension.socket.api.connection.RequesterConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.IOException;
import java.io.InputStream;

/**
 * Basic set of operations for socket extension
 *
 * @since 4.0
 */
public class SocketOperations {

  /**
   * Sends the data using the client associated to the {@link RequesterConnection} and
   * then blocks until a response is received or the timeout is met, in which case the
   * operation will return a {@code null} payload.
   *
   * @param content        data that will be serialized and sent through the socket.
   * @param outputEncoding encoding that will be used to serialize the {@code data} if its type is {@link String}.
   * @throws ConnectionException if the connection couldn't be established, if the remote host was unavailable.
   */
  public Result<InputStream, SocketAttributes> sendAndReceive(@Connection RequesterConnection connection,
                                                              @UseConfig RequesterConfig config,
                                                              @Content Object content,
                                                              @Optional @Summary("Encoding to use when the data to serialize is of String type") String outputEncoding)
      throws ConnectionException, IOException {
    SocketClient client = connection.getClient();

    outputEncoding = override(outputEncoding, config.getDefaultEncoding());

    client.write(content, outputEncoding);

    return Result.<InputStream, SocketAttributes>builder()
        .output(client.read())
        .attributes(client.getAttributes())
        .build();
  }

  /**
   * Sends the data using the client associated to the {@link RequesterConnection}.
   *
   * @param content        data that will be serialized and sent through the socket.
   * @param outputEncoding encoding that will be used to serialize the {@code data} if its type is {@link String}.
   * @throws ConnectionException if the connection couldn't be established, if the remote host was unavailable.
   */
  public void send(@Connection RequesterConnection connection,
                   @UseConfig RequesterConfig config,
                   @Content Object content,
                   @Optional @Summary("Encoding to use when the data to serialize is of String type") String outputEncoding)
      throws ConnectionException, IOException {

    connection.getClient().write(content, override(outputEncoding, config.getDefaultEncoding()));
  }

  private String override(String override, String defaultValue) {
    return override == null ? defaultValue : override;
  }
}
