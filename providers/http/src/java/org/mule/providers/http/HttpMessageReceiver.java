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
import org.apache.commons.httpclient.HttpParser;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.impl.ResponseOutputStream;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.ConnectException;
import org.mule.providers.tcp.TcpMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.monitor.Expirable;
import org.mule.util.monitor.ExpiryMonitor;

import javax.resource.spi.work.Work;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Properties;

/**
 * <code>HttpMessageReceiver</code> is a simple http server that can be used
 * to listen for http requests on a particular port
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class HttpMessageReceiver extends TcpMessageReceiver
{
    private ExpiryMonitor keepAliveMonitor;

    public HttpMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint)
            throws InitialisationException
    {
        super(connector, component, endpoint);
        if (((HttpConnector) connector).isKeepAlive()) {
            keepAliveMonitor = new ExpiryMonitor(1000);
        }
    }

    protected UMOTransformer getResponseTransformer() throws InitialisationException
    {
        UMOTransformer transformer = super.getResponseTransformer();
        if (transformer == null) {
            throw new InitialisationException(new Message("http", 1), this);
        }
        if (!transformer.getReturnClass().equals(String.class) && !transformer.getReturnClass().equals(byte[].class)
                && !transformer.getReturnClass().equals(Object.class)) {
            throw new InitialisationException(new Message("http", 2, getConnector().getName()), this);
        }
        return transformer;
    }

    protected Work createWork(Socket socket) throws SocketException {
        return new HttpWorker(socket);
    }

    public void doConnect() throws ConnectException
    {
        //If we already have an endpoint listening on this socket don't try and
        //start another serversocket
        if(shouldConnect()) {
            super.doConnect();
        }
    }

    protected boolean shouldConnect()
    {
        StringBuffer requestUri = new StringBuffer();
        requestUri.append(endpoint.getProtocol()).append("://");
        requestUri.append(endpoint.getEndpointURI().getHost());
        requestUri.append(":").append(endpoint.getEndpointURI().getPort());
        requestUri.append("*");
        AbstractMessageReceiver[] temp = connector.getReceivers(requestUri.toString());
        for (int i = 0; i < temp.length; i++) {
            AbstractMessageReceiver abstractMessageReceiver = temp[i];
            if(abstractMessageReceiver.isConnected()) {
                return false;
            }
        }
        return true;
    }

    public void doDispose()
    {
        if (keepAliveMonitor != null) {
            keepAliveMonitor.dispose();
        }
        super.doDispose();
    }

    private class HttpWorker extends TcpWorker implements Expirable
    {
        private boolean keepAlive = false;
        private boolean keepAliveRegistered = false;

        public HttpWorker(Socket socket) throws SocketException {
            super(socket);
            keepAlive = ((HttpConnector)connector).isKeepAlive();
            if(keepAlive) {
                socket.setKeepAlive(true);
            }
        }

        public void run()
        {
            try {
                int counter = 0;
                dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                dataOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                do {
                    // useful if keep alive is used
                    if (isServerSide() && ++counter > 500) {
                        counter = 0;
                        Thread.yield();
                    }
                    if (disposing.get() || socket.isClosed()) {
                        logger.debug("Peer closed connection");
                        break;
                    }

                    Properties headers = new Properties();
                    byte[] payload = parseRequest(dataIn, headers);
                    if (payload == null) {
                        break;
                    }

                    UMOMessageAdapter adapter = connector.getMessageAdapter(new Object[] { payload, headers });

                    boolean http11 = ((String) adapter.getProperty(HttpConnector.HTTP_VERSION_PROPERTY)).equalsIgnoreCase(HttpConstants.HTTP11);
                    if (!http11) {
                        keepAlive = adapter.getProperty(HttpConstants.HEADER_KEEP_ALIVE) != null;
                    } else {
                        String connection = (String) adapter.getProperty(HttpConstants.HEADER_CONNECTION);
                        if (connection != null && connection.equalsIgnoreCase("close")) {
                            keepAlive = false;
                        } else {
                            keepAlive = true;
                        }
                    }
                    if (keepAlive && !keepAliveRegistered) {
                        keepAliveRegistered = true;
                        if (keepAliveMonitor != null) {
                            keepAliveMonitor.addExpirable(((HttpConnector) connector).getKeepAliveTimeout(), this);
                        } else {
                            logger.info("Request has Keep alive set but the HttpConnector has keep alive disables");
                            keepAlive = false;
                        }
                    }

                    if (adapter != null) {
                        UMOMessage message = new MuleMessage(adapter);

                        if (logger.isDebugEnabled()) {
                            logger.debug(message.getProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
                        }
                        OutputStream os = new ResponseOutputStream(dataOut, socket);

                        //determine if the request path on this request denotes a different receiver
                        AbstractMessageReceiver receiver = getTargetReceiver(message, endpoint);
                        UMOMessage returnMessage = receiver.routeMessage(message, endpoint.isSynchronous(), os);

                        if (returnMessage == null) {
                            returnMessage = new MuleMessage("", null);
                        }

                        // Do response code mapping
                        // This is handled generically by handleExcetion method
                        // if(returnMessage.getExceptionPayload()!=null) {
                        // String responseCode =
                        // ExceptionHelper.getErrorMapping(connector.getProtocol(),
                        // returnMessage.getExceptionPayload().getCode());
                        // returnMessage.setIntProperty(HttpConnector.HTTP_STATUS_PROPERTY,
                        // Integer.parseInt(responseCode));
                        // //returnMessage = new
                        // MuleMessage(returnMessage.getExceptionPayload().getMessage(),
                        // returnMessage.getProperties());
                        // }
                        RequestContext.rewriteEvent(returnMessage);

                        Object response = responseTransformer.transform(returnMessage.getPayload());
                        if (response instanceof byte[]) {
                            dataOut.write((byte[]) response);
                        } else {
                            dataOut.write(response.toString().getBytes());
                        }
                        dataOut.flush();
                        if (keepAliveMonitor != null) {
                            keepAliveMonitor.resetExpirable(this);
                        }
                    }
                } while (socket.isConnected() && keepAlive);
                System.out.println("");
            } catch (Exception e) {
                keepAlive = false;
                handleException(e);
            } finally {
                if (keepAliveMonitor != null) {
                    keepAliveMonitor.removeExpirable(this);
                }
                dispose();
            }
        }

        public void expired()
        {
            logger.debug("Keep alive timed out");
            dispose();
        }
    }

    protected AbstractMessageReceiver getTargetReceiver(UMOMessage message, UMOEndpoint endpoint) throws ConnectException {

    String path = (String)message.getProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
        int i = path.indexOf("?");
        if(i > -1) path = path.substring(0, i);

        StringBuffer requestUri = new StringBuffer();
        requestUri.append(endpoint.getProtocol()).append("://");
        requestUri.append(endpoint.getEndpointURI().getHost());
        requestUri.append(":").append(endpoint.getEndpointURI().getPort());
        //first check there is a receive on the root address
        AbstractMessageReceiver receiver = connector.getReceiver(requestUri.toString());
        //If no receiver on the root and there is a request path, look up the received based on the
        //root plus request path
        if(receiver==null && !"/".equals(path)) {
            //remove anything after the last '/'
            int x = path.lastIndexOf("/");
            if(x > 1 && path.indexOf(".") > x) {
                requestUri.append(path.substring(0, x));
            } else {
                requestUri.append(path);
            }
            receiver = connector.getReceiver(requestUri.toString());
        }
        if(receiver==null) {

            throw new ConnectException(new Message(Messages.CANNOT_BIND_TO_ADDRESS_X, requestUri.toString()), this);
        }
        return receiver;
    }

    protected byte[] parseRequest(DataInputStream is, Properties p) throws IOException
    {
        byte[] payload;
//        if(is.available()==0) {
//            return null;
//        }
        String line = null;
        try {
            line = HttpParser.readLine(is);
        } catch (SocketException e) {
            return null;
        } catch (SocketTimeoutException e) {
            if(logger.isTraceEnabled()) logger.trace("Socket timeout on: " + this.getEndpoint().getEndpointURI().getAddress());
            return null;
        }

        if (line == null) {
            return null;
        }

        int space1 = line.indexOf(" ");
        int space2 = line.indexOf(" ", space1 + 1);
        if (space1 == -1 || space2 == -1) {
            throw new IOException("Http message header line is malformed: " + line);
        }
        String method = line.substring(0, space1);
        String request = line.substring(space1 + 1, space2);
        String httpVersion = line.substring(space2 + 1);

        p.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, method);
        p.setProperty(HttpConnector.HTTP_REQUEST_PROPERTY, request);
        p.setProperty(HttpConnector.HTTP_VERSION_PROPERTY, httpVersion);

        // Read headers from the request as set them as properties on the event
        Header[] headers = HttpParser.parseHeaders(is);
        String name;
        for (int i = 0; i < headers.length; i++) {
            name = headers[i].getName();
            if (name.startsWith("X-" + MuleProperties.PROPERTY_PREFIX)) {
                name = name.substring(2);
            }
            p.setProperty(name, headers[i].getValue());
        }

        if (method.equals(HttpConstants.METHOD_GET)) {
            payload = request.getBytes();
        } else {
            boolean contentLengthNotSet = p.getProperty(HttpConstants.HEADER_CONTENT_LENGTH, null) == null;
            int contentLength = Integer.parseInt(p.getProperty(HttpConstants.HEADER_CONTENT_LENGTH,
                                                               String.valueOf(1024 * 32)));

            byte[] buffer = new byte[contentLength];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = 0;
            int bytesWritten = 0;
            // Ensure we read all bytes, http connections may be slow
            // to send all bytes in consistent stream. I've only seen
            // this when using Axis...
            if(contentLengthNotSet) {
                //maybe we should error here??
                logger.error("Header Content-Length not set on http request. Will still read content until end of stream.");
                while (bytesWritten != contentLength) {
                    len = is.read(buffer);
                    if (len != -1) {
                        baos.write(buffer, 0, len);
                        bytesWritten += len;
                        if (contentLengthNotSet) {
                            contentLength = bytesWritten;
                        }
                    }
                }
                payload = baos.toByteArray();
                baos.close();
            } else {
                is.readFully(buffer);
                payload = buffer;
            }
        }
        return payload;
    }

}
