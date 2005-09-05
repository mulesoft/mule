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

import javax.resource.spi.work.Work;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * <code>HttpMessageReceiver</code> is a simple http server that can be used
 * to listen for http requests on a particular port
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class HttpMessageReceiver extends TcpMessageReceiver {
    //private ExpiryMonitor keepAliveMonitor;

    public HttpMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint)
            throws InitialisationException {
        super(connector, component, endpoint);
//        if (((HttpConnector) connector).isKeepAlive()) {
//            keepAliveMonitor = new ExpiryMonitor(1000);
//        }
    }

    protected UMOTransformer getResponseTransformer() throws InitialisationException {
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

    public void doDispose() {
//        if (keepAliveMonitor != null) {
//            keepAliveMonitor.dispose();
//        }
        super.doDispose();
    }

    private class HttpWorker extends TcpWorker implements Expirable {

        public HttpWorker(Socket socket) throws SocketException {
            super(socket);
            boolean keepAlive = ((HttpConnector) connector).isKeepAlive();
            if (keepAlive) {
                socket.setKeepAlive(true);
            }
        }

        public void run() {
            boolean keepAlive;
            try {
                dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                dataOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                do {
                    if (disposing.get() || socket.isClosed()) {
                        logger.debug("Peer closed connection");
                        break;
                    }

                    Properties headers = new Properties();
                    byte[] payload = parseRequest(dataIn, headers);
                    if (payload == null) {
                        break;
                    }

                    UMOMessageAdapter adapter = connector.getMessageAdapter(new Object[]{payload, headers});

                    keepAlive = adapter.getBooleanProperty(HttpConstants.HEADER_KEEP_ALIVE, true);
                    //Removed the keep alive monitoring stuff for now
                    //Most other http servers tend not to worry about the keep-alive time out
                    //nstead just wait for the client to disconnect
//                    if (keepAlive && !keepAliveRegistered) {
//                        keepAliveRegistered = true;
//                        if (keepAliveMonitor != null) {
//                            keepAliveMonitor.addExpirable(((HttpConnector) connector).getKeepAliveTimeout(), this);
//                        } else {
//                            logger.info("Request has Keep alive set but the HttpConnector has keep alive disables");
//                            keepAlive = false;
//                        }
//                    }

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

                    RequestContext.rewriteEvent(returnMessage);

                    Object response = responseTransformer.transform(returnMessage.getPayload());
                    if (response instanceof byte[]) {
                        dataOut.write((byte[]) response);
                    } else {
                        dataOut.write(response.toString().getBytes());
                    }
                    dataOut.flush();
//                        if (keepAliveMonitor != null) {
//                            keepAliveMonitor.resetExpirable(this);
//                        }
                } while (socket.isConnected() && keepAlive);
            } catch (Exception e) {
                keepAlive = false;
                handleException(e);
            } finally {
//                if (keepAliveMonitor != null) {
//                    keepAliveMonitor.removeExpirable(this);
//                }
                dispose();
            }
        }

        public void expired() {
            logger.debug("Keep alive timed out");
            dispose();
        }
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
        if (receiver == null) {

            throw new ConnectException(new Message(Messages.CANNOT_BIND_TO_ADDRESS_X, requestUri.toString()), this);
        }
        return receiver;
    }

    protected byte[] parseRequest(InputStream is, Properties p) throws IOException
    {
        RequestInputStream req = new RequestInputStream(is);
        byte[] payload;
        String startLine = null;
        do {
            try {
                startLine = req.readline();
            } catch (IOException e) {
                logger.debug(e.getMessage());
            }
            if (startLine == null) return null;
        } while (startLine.trim().length() == 0);

        StringTokenizer tokenizer = new StringTokenizer(startLine);
        String method = tokenizer.nextToken();
        String request = tokenizer.nextToken();
        String httpVersion = tokenizer.nextToken();

        p.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, method);
        p.setProperty(HttpConnector.HTTP_REQUEST_PROPERTY, request);
        p.setProperty(HttpConnector.HTTP_VERSION_PROPERTY, httpVersion);

        // Read headers from the request as set them as properties on the event
        readHeaders(req, p);

        if (method.equals(HttpConstants.METHOD_GET)) {
            payload = request.getBytes();
        } else {
            String contentLengthHeader = p.getProperty(HttpConstants.HEADER_CONTENT_LENGTH, null);
            if (contentLengthHeader == null) throw new IllegalStateException(HttpConstants.HEADER_CONTENT_LENGTH + " header must be set");

            int contentLength = Integer.parseInt(contentLengthHeader);
            byte[] buffer = new byte[ contentLength ];

            int length = -1;
            int offset = req.read(buffer);
            while (offset >= 0 && offset < buffer.length) {
                length = req.read(buffer, offset, buffer.length - offset);
                if (length == -1) {
                    break;
                }
                offset += length;
            }
            payload = buffer;
        }
        return payload;
    }

    private void readHeaders(RequestInputStream is, Properties p) throws IOException {
        String currentKey = null;
        while (true) {
            String line = is.readline();
            if ((line == null) || (line.length() == 0)) {
                break;
            }

            if (!Character.isSpaceChar(line.charAt(0))) {
                int index = line.indexOf(':');
                if (index >= 0) {
                    currentKey = line.substring(0, index).trim();
                    if (currentKey.startsWith("X-" + MuleProperties.PROPERTY_PREFIX)) {
                        currentKey = currentKey.substring(2);
                    }
                    String value = line.substring(index + 1).trim();
                    p.setProperty(currentKey, value);
                }
            } else if (currentKey != null) {
                String value = p.getProperty(currentKey);
                p.setProperty(currentKey, value + "\r\n\t" + line.trim());
            }
        }

    }
}
