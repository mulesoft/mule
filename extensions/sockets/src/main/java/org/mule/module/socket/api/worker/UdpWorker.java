/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.socket.api.worker;

import static java.lang.String.format;
import static java.util.Arrays.copyOf;
import static org.mule.module.socket.internal.SocketUtils.createMuleMessage;
import static org.mule.module.socket.internal.SocketUtils.createPacket;
import static org.mule.module.socket.internal.SocketUtils.getUdpAllowedByteArray;

import org.mule.module.socket.api.ImmutableSocketAttributes;
import org.mule.module.socket.api.SocketAttributes;
import org.mule.runtime.api.execution.CompletionHandler;
import org.mule.runtime.api.execution.ExceptionCallback;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.core.api.serialization.ObjectSerializer;
import org.mule.runtime.extension.api.runtime.MessageHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One worker is created per received package. If the other end of the connection is awaiting
 * for a response, one will be sent but not from the same listener socket the source has. The response
 * will be sent from a new different {@link DatagramSocket} bound to a port choose by the system.
 */
public final class UdpWorker extends SocketWorker
{

    private static final Logger LOGGER = LoggerFactory.getLogger(UdpWorker.class);
    private final DatagramSocket socket;
    private final DatagramPacket packet;
    private final ObjectSerializer objectSerializer;

    public UdpWorker(DatagramSocket socket, DatagramPacket packet, ObjectSerializer objectSerializer, MessageHandler<InputStream, SocketAttributes> messageHandler)
    {
        super(messageHandler);
        this.socket = socket;
        this.packet = packet;
        this.objectSerializer = objectSerializer;
    }

    @Override
    public void run()
    {
        SocketAttributes attributes = new ImmutableSocketAttributes(packet);
        InputStream content = new ByteArrayInputStream(copyOf(packet.getData(), packet.getLength()));
        messageHandler.handle(createMuleMessage(content, attributes), new CompletionHandler<MuleEvent, Exception, MuleEvent>()
        {
            @Override
            public void onCompletion(MuleEvent muleEvent, ExceptionCallback<MuleEvent, Exception> exceptionCallback)
            {
                try
                {
                    byte[] byteArray = getUdpAllowedByteArray(muleEvent.getMessage().getPayload(), encoding, objectSerializer);
                    DatagramPacket sendPacket = createPacket(byteArray);
                    sendPacket.setSocketAddress(packet.getSocketAddress());
                    socket.send(sendPacket);
                }
                catch (IOException e)
                {
                    exceptionCallback.onException(new IOException(
                            format("An error occurred while sending UDP packet to address '%s'",
                                   packet.getSocketAddress().toString(), e))
                    );
                }
            }

            @Override
            public void onFailure(Exception e)
            {
                LOGGER.error("UDP worker will not answer back due an exception was received", e);
            }
        });
    }

    @Override
    public void release()
    {
        dispose();
    }

    @Override
    public void dispose()
    {
        if (socket != null && !socket.isClosed())
        {
            try
            {
                socket.close();
            }
            catch (Exception e)
            {
                LOGGER.error("UDP worker failed closing socket", e);
            }
        }
    }
}
