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
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.transformer.UMOTransformer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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

    private class HttpWorker implements Runnable, Disposable
    {
        Socket socket = null;

        public HttpWorker(Socket socket)
        {
            super();
            this.socket = socket;
        }

        public void dispose()
        {
            try
            {
                if (socket != null) {
                    logger.info("Closing listener: " + socket.getLocalAddress());
                    socket.close();
                }
            } catch (IOException e)
            {
                logger.error("Socket close failed with: " + e);
            }
            socket = null;
        }

        /**
         * Accept requests from a given TCP port
         */
        public void run()
        {
            DataInputStream dataIn = null;
            DataOutputStream dataOut = null;
            try
            {
                dataIn = new DataInputStream(socket.getInputStream());
                dataOut = new DataOutputStream(socket.getOutputStream());
                UMOMessageAdapter adapter = connector.getMessageAdapter(dataIn);
                UMOMessage message = new MuleMessage(adapter);

                if(logger.isDebugEnabled()) {
                    logger.debug((String)message.getProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
                }
                OutputStream os = new ResponseOutputStream(dataOut, socket);
                UMOMessage returnMessage = routeMessage(message, connector.isSynchronous(), os);
                if (returnMessage == null)
                {
                    returnMessage = new MuleMessage("", null);
                }
                RequestContext.rewriteEvent(returnMessage);
                String responseText = (String)getResponseTransformer().transform(returnMessage.getPayload());
                dataOut.write(responseText.getBytes());
                dataOut.flush();

            } catch (Exception e)
            {
                handleException("Failed to process Http Request on: "
                        + (socket != null ? socket.getInetAddress().toString() : "null"),
                        e);
            } finally
            {
                try
                {
                    if (dataIn != null) dataIn.close();
                    if (dataOut != null) dataOut.close();
                    socket.close();

                } catch (Exception e)
                {
                    logger.error("Socket close failed with: " + e);
                }
            }
        }
    }
}