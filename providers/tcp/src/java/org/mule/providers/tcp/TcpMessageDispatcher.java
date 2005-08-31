/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.tcp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.Utility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.*;

/**
 * <code>TcpMessageDispatcher</code> will send transformed mule events over
 * tcp.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class TcpMessageDispatcher extends AbstractMessageDispatcher
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(TcpMessageDispatcher.class);

    private TcpConnector connector;

    public TcpMessageDispatcher(TcpConnector connector)
    {
        super(connector);
        this.connector = connector;
    }

    protected Socket initSocket(String endpoint) throws IOException, URISyntaxException
    {
        URI uri = new URI(endpoint);
        int port = uri.getPort();
        InetAddress inetAddress = InetAddress.getByName(uri.getHost());
        Socket socket = createSocket(port, inetAddress);
        socket.setReuseAddress(true);
        socket.setReceiveBufferSize(connector.getBufferSize());
        socket.setSendBufferSize(connector.getBufferSize());
        socket.setSoTimeout(connector.getTimeout());
        return socket;
    }

    public void doDispatch(UMOEvent event) throws Exception
    {
        Socket socket = null;
        try {
            Object payload = event.getTransformedMessage();
            socket = initSocket(event.getEndpoint().getEndpointURI().getAddress());
            write(socket, payload);
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    protected Socket createSocket(int port, InetAddress inetAddress) throws IOException
    {
        return new Socket(inetAddress, port);
    }

    protected void write(Socket socket, Object data) throws IOException
    {
        TcpProtocol protocol = connector.getTcpProtocol();
        byte[] binaryData;
        if (data instanceof String) {
            binaryData = data.toString().getBytes();
        } else if (data instanceof byte[]) {
            binaryData = (byte[]) data;
        } else {
            binaryData = Utility.objectToByteArray(data);
        }
        BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
        protocol.write(bos, binaryData);
        bos.flush();
    }

    public UMOMessage doSend(UMOEvent event) throws Exception
    {
        Socket socket = null;
        try {
            Object payload = event.getTransformedMessage();
            socket = initSocket(event.getEndpoint().getEndpointURI().getAddress());

            write(socket, payload);
            // If we're doing sync receive try and read return info from socket
            if (useRemoteSync(event)) {
                try {
                    byte[] result = receive(socket, event.getEndpoint().getRemoteSyncTimeout());
                    if (result == null) {
                        return null;
                    }
                    return (UMOMessage)new MuleMessage(connector.getMessageAdapter(result));
                } catch (SocketTimeoutException e) {
                    // we dont necesarily expect to receive a resonse here
                    logger.info("Socket timed out normally while doing a synchronous receive on endpointUri: "
                            + event.getEndpoint().getEndpointURI());
                    return null;
                }
            } else {
                return event.getMessage();
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    protected byte[] receive(Socket socket, int timeout) throws IOException
    {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        if (timeout >= 0) {
            socket.setSoTimeout(timeout);
        }
        return connector.getTcpProtocol().read(dis);
    }

    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception
    {
        Socket socket = null;
        try {
            socket = initSocket(endpointUri.getAddress());
            try {
                byte[] result = receive(socket, (int) timeout);
                if (result == null) {
                    return null;
                }
                UMOMessage message = new MuleMessage(connector.getMessageAdapter(result));
                return message;
            } catch (SocketTimeoutException e) {
                // we dont necesarily expect to receive a resonse here
                logger.info("Socket timed out normally while doing a synchronous receive on endpointUri: "
                        + endpointUri);
                return null;
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    public Object getDelegateSession() throws UMOException
    {
        return null;
    }

    public UMOConnector getConnector()
    {
        return connector;
    }

    public void doDispose()
    {
    }
}
