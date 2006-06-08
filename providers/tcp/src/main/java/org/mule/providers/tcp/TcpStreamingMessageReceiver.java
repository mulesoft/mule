/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */
package org.mule.providers.tcp;

import org.apache.commons.lang.StringUtils;
import org.mule.config.i18n.Message;
import org.mule.impl.MuleMessage;
import org.mule.providers.ConnectException;
import org.mule.providers.PollingMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;

/**
 * <code>TcpStreamingMessageReceiver</code> establishes a tcp client
 * connection to an external server and reads the streaming data.  No
 * polling frequency is used since with blocking i/o reads will block,
 * and with non-blocking i/o reads will occur when data is available.
 * Causing delays between read attempts is unnecessary, so this forces
 * the pollingFrequency property to zero so no pause occurs in the
 * PollingMessageReceiver class.
 *
 * @author <a href="mailto:rlucente@xecu.net">Rich Lucente</a>
 * @version $Revision$
 */
public class TcpStreamingMessageReceiver extends PollingMessageReceiver
{
    protected Socket clientSocket = null;

    protected DataInputStream dataIn = null;

    protected TcpProtocol protocol = null;

    public TcpStreamingMessageReceiver(UMOConnector connector,
                                       UMOComponent component, UMOEndpoint endpoint)
            throws InitialisationException
    {
        this(connector, component, endpoint, new Long(0));
    }

    private TcpStreamingMessageReceiver(UMOConnector connector,
                                        UMOComponent component, UMOEndpoint endpoint, Long frequency)
            throws InitialisationException
    {
        super(connector, component, endpoint, frequency);
        protocol = ((TcpConnector) connector).getTcpProtocol();
        setFrequency(0);
    }

    public void poll() throws Exception
    {
        setFrequency(0);  // make sure this is zero and not overridden via config
        byte[] data = protocol.read(dataIn);
        if (data != null) {
            UMOMessageAdapter adapter = connector.getMessageAdapter(data);
            UMOMessage message = new MuleMessage(adapter);
            routeMessage(message, endpoint.isSynchronous());
        }
    }

    public void doConnect() throws ConnectException
    {
        URI uri = endpoint.getEndpointURI().getUri();
        String host = StringUtils.defaultIfEmpty(uri.getHost(), "localhost");

        try {
            logger.debug("Attempting to connect to server socket");
            InetAddress inetAddress = InetAddress.getByName(host);
            clientSocket = new Socket(inetAddress, uri.getPort());
            TcpConnector connector = (TcpConnector) this.connector;
            clientSocket.setReceiveBufferSize(connector.getBufferSize());
            clientSocket.setSendBufferSize(connector.getBufferSize());
            clientSocket.setSoTimeout(connector.getReceiveTimeout());

            dataIn = new DataInputStream(new BufferedInputStream(clientSocket
                    .getInputStream()));
            logger.debug("Connected to server socket");
        } catch (Exception e) {
            logger.error(e);
            throw new ConnectException(new Message("tcp", 1, uri), e, this);
        }
    }

    public void doDisconnect() throws Exception
    {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.shutdownInput();
                clientSocket.shutdownOutput();
                clientSocket.close();
            }
        } finally {
            clientSocket = null;
            dataIn = null;
            logger.info("Closed tcp client socket");
        }
    }
}
