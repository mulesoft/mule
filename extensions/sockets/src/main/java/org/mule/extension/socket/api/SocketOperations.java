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
import org.mule.extension.socket.api.metadata.SocketMetadataResolver;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.OperationResult;

import java.io.IOException;

/**
 * Basic set of operations for socket extension
 *
 * @since 4.0
 */
public class SocketOperations {

  /**
   * Sends the data using the client associated to the {@link RequesterConnection}.
   * <p>
   * If {@code hasResponse} is set, the operation blocks until a response is received or the timeout is met, in which case the
   * operation will return a {@link MuleMessage} with {@code null} payload.
   *
   * @param content data that will be serialized and sent through the socket.
   * @param hasResponse whether the operation should await for a response or not
   * @param outputEncoding encoding that will be used to serialize the {@code data} if its type is {@link String}.
   * @param muleMessage if there is no response expected, the outcome of the operation will be the same {@link MuleMessage} as the
   *        input.
   * @throws ConnectionException if the connection couldn't be established, if the remote host was unavailable.
   */
  @MetadataScope(outputResolver = SocketMetadataResolver.class, keysResolver = SocketMetadataResolver.class)
  public OperationResult<?, ?> send(@Connection RequesterConnection connection, @UseConfig RequesterConfig config,
                                    @Optional(defaultValue = "#[payload]") @XmlHints(allowReferences = false) Object content,
                                    @Optional @Summary("Encoding to use when the data to serialize is of String type") String outputEncoding,
                                    @MetadataKeyId String hasResponse, MuleMessage muleMessage)
      throws ConnectionException, IOException {
    SocketClient client = connection.getClient();

    if (outputEncoding == null) {
      outputEncoding = config.getDefaultEncoding();
    }

    client.write(content, outputEncoding);

    return Boolean.valueOf(hasResponse)
        ? OperationResult.builder().output(client.read()).attributes(client.getAttributes()).build()
        : OperationResult.builder(muleMessage).build();
  }
}
