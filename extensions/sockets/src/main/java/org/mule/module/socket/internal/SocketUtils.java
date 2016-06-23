/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.socket.internal;

import static java.lang.String.format;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.module.socket.api.connection.AbstractSocketConnection;
import org.mule.module.socket.api.exceptions.UnresolvableHostException;
import org.mule.module.socket.api.socket.tcp.TcpSocketProperties;
import org.mule.module.socket.api.socket.udp.UdpSocketProperties;
import org.mule.module.socket.api.source.SocketAttributes;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.serialization.ObjectSerializer;
import org.mule.runtime.core.transformer.types.DataTypeFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;

public final class SocketUtils
{

    private SocketUtils()
    {

    }

    private static final String SOCKET_COULD_NOT_BE_CREATED = "%s Socket could not be created correctly";

    /**
     * UDP doesn't allow streaming and it always sends payload when dealing with a {@link MuleMessage}
     */
    public static byte[] getUdpAllowedByteArray(Object data, ObjectSerializer objectSerializer) throws IOException
    {
        return getByteArray(data, true, false, objectSerializer);
    }

    public static byte[] getByteArray(Object data, boolean payloadOnly, boolean streamingIsAllowed, ObjectSerializer objectSerializer) throws IOException
    {
        if (data instanceof InputStream && !streamingIsAllowed)
        {
            throw new IOException("Streaming is not allowed with this configuration");
        }
        else if (data instanceof MuleMessage)
        {
            if (payloadOnly)
            {
                return getByteArray(((MuleMessage) data).getPayload(), payloadOnly, streamingIsAllowed, objectSerializer);
            }
            else
            {
                return objectSerializer.serialize(data);
            }
        }
        else if (data instanceof byte[])
        {
            return (byte[]) data;
        }
        else if (data instanceof String)
        {
            // TODO Use encoding MULE-9900
            return ((String) data).getBytes();
        }
        else if (data instanceof Serializable)
        {
            return objectSerializer.serialize(data);
        }

        throw new IllegalArgumentException(format("Cannot serialize data: '%s'", data));
    }

    /**
     * @param connection delegates connectin's validation on {@link AbstractSocketConnection#validate()}
     * @return a {@link ConnectionValidationResult} with the outcome of the validation
     */
    public static ConnectionValidationResult validate(AbstractSocketConnection connection)
    {
        return connection.validate();
    }

    public static MuleMessage<InputStream, SocketAttributes> createMuleMessage(InputStream content, SocketAttributes attributes, MuleContext muleContext)
    {
        DataType dataType = DataTypeFactory.create(InputStream.class);
        Object payload = NullPayload.getInstance();
        MuleMessage<InputStream, SocketAttributes> message;

        if (content != null)
        {
            payload = content;
        }

        message = (MuleMessage) new DefaultMuleMessage(payload, dataType, attributes, muleContext);
        return message;
    }

    /**
     * Creates a {@link DatagramPacket} with the size of the content, addressed to
     * the port and address of the client.
     *
     * @param content that is going to be sent inside the packet
     * @return a packet ready to be sent
     * @throws UnresolvableHostException
     */
    public static DatagramPacket createPacket(byte[] content) throws UnresolvableHostException
    {
        return new DatagramPacket(content, content.length);
    }

    /**
     * @return a packet configured to be used for receiving purposes
     * @throws UnresolvableHostException
     */
    public static DatagramPacket createPacket(int bufferSize) throws UnresolvableHostException
    {
        return new DatagramPacket(new byte[bufferSize], bufferSize);
    }


    /**
     * Sets the configuration parameters into the socket.
     *
     * @param socket           UDP Socket
     * @param socketProperties Configuration properties
     * @throws ConnectionException
     */
    public static void configureConnection(DatagramSocket socket, UdpSocketProperties socketProperties) throws ConnectionException
    {
        checkArgument(socket != null, "Null socket found. UDP Socket must be created before being configured");

        try
        {
            if (socketProperties.getSendBufferSize() != null)
            {
                socket.setSendBufferSize(socketProperties.getSendBufferSize());
            }

            if (socketProperties.getReceiveBufferSize() != null)
            {
                socket.setReceiveBufferSize(socketProperties.getReceiveBufferSize());
            }

            if (socketProperties.getClientTimeout() != null)
            {
                socket.setSoTimeout(socketProperties.getClientTimeout());
            }

            socket.setBroadcast(socketProperties.getBroadcast());
            socket.setReuseAddress(socketProperties.getReuseAddress());
        }
        catch (Exception e)
        {
            throw new ConnectionException(format(SOCKET_COULD_NOT_BE_CREATED, "UDP"), e);
        }
    }

    /**
     * Sets the configuration parameters into the socket
     *
     * @param socket           TCP Socket
     * @param socketProperties Configuration properties
     * @throws ConnectionException
     */
    public static void configureConnection(Socket socket, TcpSocketProperties socketProperties) throws ConnectionException

    {
        try
        {
            if (socketProperties.getSendBufferSize() != null)
            {
                socket.setSendBufferSize(socketProperties.getSendBufferSize());
            }

            if (socketProperties.getReceiveBufferSize() != null)
            {
                socket.setReceiveBufferSize(socketProperties.getReceiveBufferSize());
            }

            if (socketProperties.getClientTimeout() != null)
            {
                socket.setSoTimeout(socketProperties.getClientTimeout());
            }

            if (socketProperties.getKeepAlive() != null)
            {
                socket.setKeepAlive(socketProperties.getKeepAlive());
            }

            if (socketProperties.getLinger() != null)
            {
                socket.setSoLinger(true, socketProperties.getLinger());
            }
        }
        catch (SocketException e)
        {
            throw new ConnectionException(format(SOCKET_COULD_NOT_BE_CREATED, "TCP"), e);
        }

        try
        {
            socket.setTcpNoDelay(socketProperties.getSendTcpNoDelay());
        }
        catch (SocketException e)
        {
            // MULE-2800 - Bug in Solaris
        }
    }
}
