/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.socket.api;

import org.mule.module.socket.api.client.SocketClient;
import org.mule.module.socket.api.connection.RequesterConnection;
import org.mule.module.socket.api.metadata.SocketMetadataResolver;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.NoRef;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.io.IOException;

/**
 * Basic set of operations for socket extension
 *
 * @since 4.0
 */
public class SocketOperations
{

    /**
     * Sends the data using the client associated to the {@link RequesterConnection}.
     *
     * If {@code hasResponse} is set, the operation blocks until  a response is received or the timeout is met,
     * in which case the operation will return a {@link MuleMessage}
     * with {@link NullPayload} as payload.
     *
     * @param data        that will be serialized and sent through the socket.
     * @param hasResponse whether the operation should await for a response or not
     * @param muleMessage if there is no response expected, the outcome of the operation will be
     *                    the same {@link MuleMessage} as the input.
     * @throws ConnectionException if the connection couldn't be established, if the remote host was unavailable.
     */
    @MetadataScope(outputResolver = SocketMetadataResolver.class, keysResolver = SocketMetadataResolver.class)
    public MuleMessage<?, ?> send(@Connection RequesterConnection connection,
                                  @Optional(defaultValue = "#[payload]") @NoRef Object data,
                                  String hasResponse, // TODO Add metadata https://www.mulesoft.org/jira/browse/MULE-9894
                                  @Optional(defaultValue = "UTF-8") String encoding, //TODO support encoding MULE-9900
                                  MuleMessage<?, ?> muleMessage) throws ConnectionException, IOException
    {
        SocketClient client = connection.getClient();

        client.write(data);

        return Boolean.valueOf(hasResponse) ?
               MuleMessage.builder().payload(client.read()).attributes(client.getAttributes()).build() :
               muleMessage;
    }
}
