/* 

 * $Header$

 * $Revision$

 * $Date$

 * ------------------------------------------------------------------------------------------------------

 * 

 * Copyright (c) Cubis Limited. All rights reserved.

 * http://www.cubis.co.uk 

 * 

 * The software in this package is published under the terms of the BSD

 * style license a copy of which has been included with this distribution in

 * the LICENSE.txt file. 

 *

 */
package org.mule.providers.http;

import org.mule.InitialisationException;
import org.mule.MuleRuntimeException;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.impl.ResponseOutputStream;
import org.mule.providers.AbstractConnector;
import org.mule.providers.tcp.TcpMessageReceiver;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.transformer.UMOTransformer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * <code>HttpMessageReceiver</code> is a simple http server that can be used to
 * listen for http requests on a particular port
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class HttpMessageReceiver extends TcpMessageReceiver
{
    private UMOTransformer responseTransformer = null;

    public HttpMessageReceiver(AbstractConnector connector, UMOComponent component, UMOEndpoint endpoint)
            throws InitialisationException
    {
        super(connector, component, endpoint);

    }

    private UMOTransformer getResponseTransformer() throws InitialisationException
    {
        if (responseTransformer == null)
        {
            responseTransformer = (AbstractEventAwareTransformer) ((AbstractConnector) connector).getDefaultResponseTransformer();
            if (responseTransformer == null)
            {
                throw new InitialisationException("Response transformer is required for the http endpoint. Check the connector service descriptor");
            }
        }
        return responseTransformer;
    }

    protected Runnable createWorker(Socket socket)
    {
        return new HttpWorker(socket);
    }

    protected boolean allowFilter(UMOFilter filter) throws UnsupportedOperationException
    {
        return true;
    }

    private class HttpWorker extends TcpWorker
    {
        public HttpWorker(Socket socket)
        {
            super(socket);
        }

        public void run()
        {
            try
            {
                dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                dataOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                int counter = 0;
                while (!socket.isClosed() && !disposing.get())
                {
                    //useful if keep alive is implemented
//                    if (isServerSide() && ++counter > 500)
//                    {
//                        counter = 0;
//                        Thread.yield();
//                    }

                    UMOMessageAdapter adapter = null;
                    try
                    {
                        adapter = connector.getMessageAdapter(socket.getInputStream());
                    } catch (MuleRuntimeException e)
                    {
                        logger.debug(e.getMessage());
                    }
                    if (adapter != null)
                    {
                        UMOMessage message = new MuleMessage(adapter);

                        if (logger.isDebugEnabled())
                        {
                            logger.debug((String) message.getProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
                        }
                        OutputStream os = new ResponseOutputStream(dataOut, socket);
                        UMOMessage returnMessage = routeMessage(message, connector.isSynchronous(), os);
                        if (returnMessage == null)
                        {
                            returnMessage = new MuleMessage("", null);
                        }
                        RequestContext.rewriteEvent(returnMessage);
                        String responseText = (String) getResponseTransformer().transform(returnMessage.getPayload());
                        dataOut.write(responseText.getBytes());
                        dataOut.flush();
                        //todo keep alive
                        socket.close();
                    }
                }
            } catch (Exception e)
            {
                handleException("Failed to process tcp Request on: "
                        + (socket != null ? socket.getInetAddress().toString() : "null"),
                        e);
            } finally
            {
                dispose();
            }
        }
    }

}