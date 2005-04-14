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

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.Utility;

import java.io.*;
import java.net.*;

/**
 * <code>TcpMessageDispatcher</code> will send transformed mule events over tcp.
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

    private Socket socket;

    private InetAddress inetAddress;

    private int port;

    private PrintWriter printWriter;
    private DataOutputStream dataOut;

    private SynchronizedBoolean initialised = new SynchronizedBoolean(false);

    public TcpMessageDispatcher(TcpConnector connector)
    {
        super(connector);
        this.connector = connector;
        disposeOnCompletion = true;
    }

    protected void initialise(String endpoint) throws IOException, URISyntaxException
    {
        if (!initialised.get() || socket.isClosed())
        {
            URI uri = new URI(endpoint);
            port = uri.getPort();
            inetAddress = InetAddress.getByName(uri.getHost());
            socket = createSocket(port, inetAddress);
            socket.setReuseAddress(true);
            socket.setReceiveBufferSize(connector.getBufferSize());
            socket.setSendBufferSize(connector.getBufferSize());
            socket.setSoTimeout(connector.getTimeout());
            printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
            dataOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            initialised.set(true);
        }
    }

    public void doDispatch(UMOEvent event) throws Exception
    {
        initialise(event.getEndpoint().getEndpointURI().getAddress());
        Object payload = event.getTransformedMessage();
        write(socket, payload);
    }

    protected Socket createSocket(int port, InetAddress inetAddress) throws IOException
    {
        return new Socket(inetAddress, port);
    }

    protected void write(Socket socket, Object data) throws IOException
    {
        if (data instanceof String)
        {
            printWriter.write(data.toString());
            printWriter.flush();
        } else if (data instanceof byte[])
        {
            BufferedOutputStream bos = new BufferedOutputStream(dataOut);
            bos.write((byte[])data);
            bos.flush();
        } else {
            dataOut.write(Utility.objectToByteArray(data));
            dataOut.flush();
        }

    }

    public UMOMessage doSend(UMOEvent event) throws Exception
    {
        initialise(event.getEndpoint().getEndpointURI().getAddress());
        Object payload = event.getTransformedMessage();

        boolean syncReceive = event.getBooleanProperty(MuleProperties.MULE_SYNCHRONOUS_RECEIVE_PROPERTY,
                MuleManager.getConfiguration().isSynchronousReceive());
        
        write(socket, payload);
        //If we're doing sync receive try and read return info from socket
        if (syncReceive)
        {
            try
            {
                byte[] result = receive(socket, event.getTimeout());
                dataOut.close();
                if (result == null) return null;
                UMOMessage message = new MuleMessage(connector.getMessageAdapter(result));
                return message;
            } catch (SocketTimeoutException e)
            {
                //we dont necesarily expect to receive a resonse here
                logger.info("Socket timed out normally while doing a synchronous receive on endpointUri: " + event.getEndpoint().getEndpointURI());
                return null;
            }
        } else
        {
            dataOut.close();
            return event.getMessage();
        }
    }

    private byte[] receive(Socket socket, int timeout) throws IOException
    {

        DataInputStream dis = new DataInputStream(socket.getInputStream());
        if(timeout >= 0) {
            socket.setSoTimeout(timeout);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream(connector.getBufferSize());
        byte[] buffer = new byte[connector.getBufferSize()];
        int len = 0;
        try
        {
            while ((len = dis.read(buffer, len, buffer.length)) != 0)
            {
                if (len == -1)
                {
                    logger.debug("The socket is closed");
                    return null;
                } else
                {
                    baos.write(buffer, 0, len);
                    if(len != buffer.length) break;
                }
            }
            baos.flush();
            return baos.toByteArray();
        } finally
        {
            try
            {
                //if(dis!=null) dis.close();
                if(baos!=null) baos.close();
            } catch (IOException e)
            {
                logger.error("failed to close tcp stream: " + e);
            }
        }

    }

    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception
    {
        initialise(endpointUri.getAddress());
        int origTimeout = socket.getSoTimeout();
        int newTimeout = Integer.parseInt(String.valueOf(timeout));
        if(origTimeout!= newTimeout) {
            socket.setSoTimeout(Integer.parseInt(String.valueOf(timeout)));
        }
        try
        {
            byte[] result = receive(socket, Integer.parseInt(String.valueOf(timeout)));
            if (result == null) return null;
            UMOMessage message = new MuleMessage(connector.getMessageAdapter(new ByteArrayInputStream(result)));
            return message;
        } finally
        {
            if(origTimeout!= newTimeout) {
                socket.setSoTimeout(origTimeout);
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
        initialised.set(false);
        try
        {

            if(dataOut!=null) dataOut.close();
        } catch (IOException e)
        {
            logger.error("failed to dispose Tcp dispatcher: " + e.getMessage());
        }
        if(printWriter!=null) printWriter.close();

        try
        {

            if(socket!=null) socket.close();
        } catch (IOException e)
        {
            logger.error("failed to dispose Tcp dispatcher: " + e.getMessage());
        }
    }
}
