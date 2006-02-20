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
 *
 */
package org.mule.providers.http;

import org.apache.commons.httpclient.Header;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.ConnectException;
import org.mule.providers.streaming.StreamMessageAdapter;
import org.mule.providers.tcp.TcpMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;

import javax.resource.spi.work.Work;
import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * <code>HttpMessageReceiver</code> is a simple http server that can be used
 * to listen for http requests on a particular port
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class HttpMessageReceiver extends TcpMessageReceiver {

    public HttpMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint)
            throws InitialisationException {
        super(connector, component, endpoint);
    }

    protected Work createWork(Socket socket) throws IOException {
        return new HttpWorker(socket);
    }

    public void doConnect() throws ConnectException {
        //If we already have an endpoint listening on this socket don't try and
        //start another serversocket
        if (shouldConnect()) {
            super.doConnect();
        }
    }

    protected boolean shouldConnect() {
        StringBuffer requestUri = new StringBuffer();
        requestUri.append(endpoint.getProtocol()).append("://");
        requestUri.append(endpoint.getEndpointURI().getHost());
        requestUri.append(":").append(endpoint.getEndpointURI().getPort());
        requestUri.append("*");
        AbstractMessageReceiver[] temp = connector.getReceivers(requestUri.toString());
        for (int i = 0; i < temp.length; i++) {
            AbstractMessageReceiver abstractMessageReceiver = temp[i];
            if (abstractMessageReceiver.isConnected()) {
                return false;
            }
        }
        return true;
    }

    private class HttpWorker implements Work {

        HttpServerConnection conn = null;
        public HttpWorker(Socket socket) throws IOException {
            conn = new HttpServerConnection(socket);
        }

        public void run() {

            try {
                do {
                    conn.setKeepAlive(false);
                    HttpRequest request = conn.readRequest();
                    if(request==null) break;
                    
                    Properties headers = new Properties();
                    for (int i = 0; i < request.getHeaders().length; i++) {
                        Header header = request.getHeaders()[i];
                        String headerName = header.getName();
                        if(headerName.startsWith("X-MULE")) {
                            headerName = headerName.replaceAll("X-MULE", "MULE");
                        }
                        headers.setProperty(headerName, header.getValue());
                    }
                    headers.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, request.getRequestLine().getMethod());
                    headers.setProperty(HttpConnector.HTTP_REQUEST_PROPERTY, request.getRequestLine().getUri());
                    headers.setProperty(HttpConnector.HTTP_VERSION_PROPERTY, request.getRequestLine().getHttpVersion().toString());

                    //todo Mule 2.0 generic way to set stream message adapter
                    UMOMessageAdapter adapter;
                    Object body = null;
                    if(endpoint.isStreaming() && request.getBody()!=null) {
                        adapter = new StreamMessageAdapter(request.getBody(), conn.getOutputStream());
                        for (Iterator iterator = headers.entrySet().iterator(); iterator.hasNext();) {
                            Map.Entry entry = (Map.Entry) iterator.next();
                            adapter.setProperty(entry.getKey(), entry.getValue());
                        }
                    } else {
                        body = request.getBodyBytes();
                        if(body==null) body = request.getRequestLine().getUri();
                        adapter = connector.getMessageAdapter(new Object[]{body, headers});
                    }
                    UMOMessage message = new MuleMessage(adapter);

                    if (logger.isDebugEnabled()) {
                        logger.debug(message.getProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
                    }

                    //determine if the request path on this request denotes a different receiver
                    AbstractMessageReceiver receiver = getTargetReceiver(message, endpoint);

                    //the respone only needs to be transformed explicitly if A) the request was not served or a null result was returned
                    HttpResponse response = null;
                    if (receiver != null) {
                        UMOMessage returnMessage = receiver.routeMessage(message, endpoint.isSynchronous(), /*todo*/ null);
                        Object tempResponse = returnMessage.getPayload();
                        //This remoes the need for users to explicitly adding the response transformer ObjectToHttpResponse in their config
                        if(tempResponse instanceof HttpResponse) {
                            response = (HttpResponse)tempResponse;
                        } else {
                            response = (HttpResponse)connector.getDefaultResponseTransformer().transform(tempResponse);
                        }
                        response.disableKeepAlive(!((HttpConnector)connector).isKeepAlive());
                    } else {
                        String failedPath = endpoint.getEndpointURI().getScheme() + "://" +
                                endpoint.getEndpointURI().getHost() + ":" + endpoint.getEndpointURI().getPort() +
                                getRequestPath(message);

                        if(logger.isDebugEnabled()) logger.debug("Failed to bind to " + failedPath);

                        response = new HttpResponse();
                        response.setStatusLine(request.getRequestLine().getHttpVersion(), HttpConstants.SC_NOT_FOUND);
                        response.setBodyString(new Message(Messages.CANNOT_BIND_TO_ADDRESS_X, failedPath).toString());
                        //The DefaultResponse Transformer will set the necessary Headers
                        response = (HttpResponse)connector.getDefaultResponseTransformer().transform(response);
                    }

                   conn.writeResponse(response);
                } while (conn.isKeepAlive());
            } catch (Exception e) {
                handleException(e);
            } finally {
                conn.close();
                conn=null;
            }
        }

        public void release() {
            conn.close();
            conn=null;
        }
    }

    protected String getRequestPath(UMOMessage message) {
        String path = (String) message.getProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
        int i = path.indexOf("?");
        if (i > -1) path = path.substring(0, i);
        return path;
    }


    protected AbstractMessageReceiver getTargetReceiver(UMOMessage message, UMOEndpoint endpoint) throws ConnectException {

        String path = (String) message.getProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
        int i = path.indexOf("?");
        if (i > -1) path = path.substring(0, i);

        StringBuffer requestUri = new StringBuffer();
        requestUri.append(endpoint.getProtocol()).append("://");
        requestUri.append(endpoint.getEndpointURI().getHost());
        requestUri.append(":").append(endpoint.getEndpointURI().getPort());
        //first check there is a receive on the root address
        AbstractMessageReceiver receiver = connector.getReceiver(requestUri.toString());
        //If no receiver on the root and there is a request path, look up the received based on the
        //root plus request path
        if (receiver == null && !"/".equals(path)) {
            //remove anything after the last '/'
            int x = path.lastIndexOf("/");
            if (x > 1 && path.indexOf(".") > x) {
                requestUri.append(path.substring(0, x));
            } else {
                requestUri.append(path);
            }
            receiver = connector.getReceiver(requestUri.toString());
        }
        return receiver;
    }
}
