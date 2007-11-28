/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.RegistryContext;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.NullSessionHandler;
import org.mule.impl.OptimizedRequestContext;
import org.mule.impl.RequestContext;
import org.mule.providers.ConnectException;
import org.mule.providers.NullPayload;
import org.mule.providers.http.i18n.HttpMessages;
import org.mule.providers.tcp.TcpMessageReceiver;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.CreateException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.MapUtils;
import org.mule.util.ObjectUtils;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.resource.spi.work.Work;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.cookie.MalformedCookieException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>HttpMessageReceiver</code> is a simple http server that can be used to
 * listen for HTTP requests on a particular port.
 */
public class HttpMessageReceiver extends TcpMessageReceiver
{
    protected final Log logger = LogFactory.getLog(getClass());

    public HttpMessageReceiver(UMOConnector connector, UMOComponent component, UMOImmutableEndpoint endpoint)
            throws CreateException
    {
        super(connector, component, endpoint);
    }

    // @Override
    protected Work createWork(Socket socket) throws IOException
    {
        return new HttpWorker(socket);
    }

    // @Override
    protected void doConnect() throws ConnectException
    {
        // If we already have an endpoint listening on this socket don't try and
        // start another serversocket
        if (this.shouldConnect())
        {
            super.doConnect();
        }
    }

    protected boolean shouldConnect()
    {
        StringBuffer requestUri = new StringBuffer(80);
        requestUri.append(endpoint.getProtocol()).append("://");
        requestUri.append(endpoint.getEndpointURI().getHost());
        requestUri.append(':').append(endpoint.getEndpointURI().getPort());
        requestUri.append('*');

        UMOMessageReceiver[] receivers = connector.getReceivers(requestUri.toString());
        for (int i = 0; i < receivers.length; i++)
        {
            if (receivers[i].isConnected())
            {
                return false;
            }
        }

        return true;
    }


    // @Override
    protected UMOMessage handleUnacceptedFilter(UMOMessage message)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Message request '" + message.getProperty(HttpConnector.HTTP_REQUEST_PROPERTY)
                    + "' is being rejected since it does not match the filter on this endpoint: " + endpoint);
        }
        message.setProperty(HttpConnector.HTTP_STATUS_PROPERTY, String.valueOf(HttpConstants.SC_NOT_ACCEPTABLE));
        return message;
    }

    protected class HttpWorker implements Work
    {
        private HttpServerConnection conn = null;
        private String cookieSpec;
        private boolean enableCookies = false;

        public HttpWorker(Socket socket) throws IOException
        {
            String encoding = endpoint.getEncoding();
            if (encoding == null)
            {
                encoding = RegistryContext.getConfiguration().getDefaultEncoding();
            }

            conn = new HttpServerConnection(socket, encoding);

            cookieSpec =
                    MapUtils.getString(endpoint.getProperties(), HttpConnector.HTTP_COOKIE_SPEC_PROPERTY,
                            ((HttpConnector) connector).getCookieSpec());
            enableCookies =
                    MapUtils.getBooleanValue(endpoint.getProperties(), HttpConnector.HTTP_ENABLE_COOKIES_PROPERTY,
                            ((HttpConnector) connector).isEnableCookies());
        }

        public void run()
        {
            try
            {
                do
                {
                    conn.setKeepAlive(false);
                    HttpRequest request = conn.readRequest();
                    if (request == null)
                    {
                        break;
                    }
                    conn.writeResponse(processRequest(request));
                }
                while (conn.isKeepAlive());
            }
            catch (Exception e)
            {
                handleException(e);
            }
            finally
            {
                logger.info("Closing HTTP connection.");

                conn.close();
                conn = null;
            }
        }

        protected HttpResponse processRequest(HttpRequest request) throws UMOException, IOException
        {
            RequestLine requestLine = request.getRequestLine();
            String method = requestLine.getMethod();

            if (method.equals(HttpConstants.METHOD_HEAD))
            {
                return doHead(requestLine);
            }
            else if (method.equals(HttpConstants.METHOD_GET)
                    || method.equals(HttpConstants.METHOD_POST)
                    || method.equals(HttpConstants.METHOD_OPTIONS)
                    || method.equals(HttpConstants.METHOD_PUT)
                    || method.equals(HttpConstants.METHOD_DELETE)
                    || method.equals(HttpConstants.METHOD_TRACE)
                    || method.equals(HttpConstants.METHOD_CONNECT))
            {
                return doRequest(request, requestLine);
            }
            else
            {
                return doBad(requestLine);
            }
        }

        protected HttpResponse doHead(RequestLine requestLine) throws UMOException
        {
            UMOMessage message = new MuleMessage(NullPayload.getInstance());
            UMOEvent event = new MuleEvent(message, endpoint, new MuleSession(message, new NullSessionHandler()), true);
            OptimizedRequestContext.unsafeSetEvent(event);
            HttpResponse response = new HttpResponse();
            response.setStatusLine(requestLine.getHttpVersion(), HttpConstants.SC_OK);
            return transformResponse(response);
        }

        protected HttpResponse doRequest(HttpRequest request,
                                         RequestLine requestLine) throws IOException, UMOException
        {
            Map headers = parseHeaders(request);

            // TODO Mule 2.0 generic way to set stream message adapter
            UMOMessageAdapter adapter = buildStandardAdapter(request, headers);
            
            UMOMessage message = new MuleMessage(adapter);
            
            if (logger.isDebugEnabled())
            {
                logger.debug(message.getProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
            }

            // determine if the request path on this request denotes a different receiver
            UMOMessageReceiver receiver = getTargetReceiver(message, endpoint);

            HttpResponse response;
            // the response only needs to be transformed explicitly if
            // A) the request was not served or B) a null result was returned
            if (receiver != null)
            {
                preRouteMessage(message);
                UMOMessage returnMessage = receiver.routeMessage(message, endpoint.isSynchronous(), null);

                Object tempResponse;
                if (returnMessage != null)
                {
                    tempResponse = returnMessage.getPayload();
                }
                else
                {
                    tempResponse = NullPayload.getInstance();
                }
                // This removes the need for users to explicitly adding the response transformer
                // ObjectToHttpResponse in their config
                if (tempResponse instanceof HttpResponse)
                {
                    response = (HttpResponse)tempResponse;
                }
                else
                {
                    response = transformResponse(returnMessage);
                }
                response.disableKeepAlive(!((HttpConnector)connector).isKeepAlive());
            }
            else
            {
                response = buildFailureResponse(requestLine, message);
            }
            return response;
        }

        protected HttpResponse doOtherValid(RequestLine requestLine, String method) throws UMOException
        {
            UMOMessage message = new MuleMessage(NullPayload.getInstance());
            UMOEvent event = new MuleEvent(message, endpoint, new MuleSession(message, new NullSessionHandler()), true);
            OptimizedRequestContext.unsafeSetEvent(event);
            HttpResponse response = new HttpResponse();
            response.setStatusLine(requestLine.getHttpVersion(), HttpConstants.SC_METHOD_NOT_ALLOWED);
            response.setBodyString(HttpMessages.methodNotAllowed(method).toString() + HttpConstants.CRLF);
            return transformResponse(response);
        }

        protected HttpResponse doBad(RequestLine requestLine) throws UMOException
        {
            UMOMessage message = new MuleMessage(NullPayload.getInstance());
            UMOEvent event = new MuleEvent(message, endpoint, new MuleSession(message, new NullSessionHandler()), true);
            OptimizedRequestContext.unsafeSetEvent(event);
            HttpResponse response = new HttpResponse();
            response.setStatusLine(requestLine.getHttpVersion(), HttpConstants.SC_BAD_REQUEST);
            response.setBodyString(HttpMessages.malformedSyntax().toString() + HttpConstants.CRLF);
            return transformResponse(response);
        }

        protected UMOMessageAdapter buildStandardAdapter(final HttpRequest request, 
                                                         final Map headers) throws MessagingException, TransformerException, IOException
        {
            final RequestLine requestLine = request.getRequestLine();
            
            sendExpect100(headers, requestLine);
            
            Object body = request.getBody();
            if (body == null)
            {
                body = requestLine.getUri();
            }
            
            return connector.getMessageAdapter(new Object[]{body, headers});
        }

        private void sendExpect100(Map headers, RequestLine requestLine)
            throws TransformerException, IOException
        {
            // respond with status code 100, for Expect handshake
            // according to rfc 2616 and http 1.1
            // the processing will continue and the request will be fully
            // read immediately after
            if (HttpConstants.HTTP11.equals(headers.get(HttpConnector.HTTP_VERSION_PROPERTY)))
            {
                // just in case we have something other than String in
                // the headers map
                String expectHeaderValue = ObjectUtils.toString(
                        headers.get(HttpConstants.HEADER_EXPECT)).toLowerCase();
                if (HttpConstants.HEADER_EXPECT_CONTINUE_REQUEST_VALUE.equals(expectHeaderValue))
                {
                    HttpResponse expected = new HttpResponse();
                    expected.setStatusLine(requestLine.getHttpVersion(), HttpConstants.SC_CONTINUE);
                    final MuleEvent event = new MuleEvent(new MuleMessage(expected), endpoint,
                            new MuleSession(component), true);
                    RequestContext.setEvent(event);
                    conn.writeResponse(transformResponse(expected));
                }
            }
        }

        protected HttpResponse buildFailureResponse(RequestLine requestLine, UMOMessage message) throws TransformerException
        {
            UMOEndpointURI uri = endpoint.getEndpointURI();
            String failedPath = uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort()
                    + getRequestPath(message);

            if (logger.isDebugEnabled())
            {
                logger.debug("Failed to bind to " + failedPath);
            }

            HttpResponse response = new HttpResponse();
            response.setStatusLine(requestLine.getHttpVersion(), HttpConstants.SC_NOT_FOUND);
            response.setBodyString(HttpMessages.cannotBindToAddress(failedPath).toString());
            RequestContext.setEvent(new MuleEvent(new MuleMessage(response), endpoint,
                    new MuleSession(component), true));
            // The DefaultResponseTransformer will set the necessary headers
            return transformResponse(response);
        }

        protected Map parseHeaders(HttpRequest request) throws MalformedCookieException
        {
            RequestLine requestLine = request.getRequestLine();
            Map headers = new HashMap();

            for (Iterator rhi = request.getHeaderIterator(); rhi.hasNext();)
            {
                Header header = (Header)rhi.next();
                String headerName = header.getName();
                Object headerValue = header.getValue();

                // fix Mule headers?
                if (headerName.startsWith("X-MULE"))
                {
                    headerName = headerName.substring(2);
                }
                // Parse cookies?
                else if (headerName.equals(HttpConnector.HTTP_COOKIES_PROPERTY))
                {
                    if (enableCookies)
                    {
                        Cookie[] cookies = CookieHelper.parseCookies(header, cookieSpec);
                        if (cookies.length > 0)
                        {
                            // yum!
                            headerValue = cookies;
                        }
                        else
                        {
                            // bad cookies?!
                            continue;
                        }
                    }
                    else
                    {
                        // no cookies for you!
                        continue;
                    }
                }

                // accept header & value
                headers.put(headerName, headerValue);
            }

            headers.put(HttpConnector.HTTP_METHOD_PROPERTY, requestLine.getMethod());
            headers.put(HttpConnector.HTTP_REQUEST_PROPERTY, requestLine.getUri());
            headers.put(HttpConnector.HTTP_VERSION_PROPERTY, requestLine.getHttpVersion().toString());
            headers.put(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY, cookieSpec);
            return headers;
        }

        /**
         * Needed for setting connection specific properties (like ssl-certificates) in {@link HttpsMessageReceiver}
         * @see HttpsMessageReceiver
         * @param message
         */
        protected void preRouteMessage(UMOMessage message)
        {
            // no op
        }

        public void release()
        {
            conn.close();
            conn = null;
        }
    }

    protected String getRequestPath(UMOMessage message)
    {
        String path = (String) message.getProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
        int i = path.indexOf('?');
        if (i > -1)
        {
            path = path.substring(0, i);
        }
        return path;
    }

    protected UMOMessageReceiver getTargetReceiver(UMOMessage message, UMOImmutableEndpoint endpoint)
            throws ConnectException
    {
        String path = (String) message.getProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
        int i = path.indexOf('?');
        if (i > -1)
        {
            path = path.substring(0, i);
        }

        StringBuffer requestUri = new StringBuffer(80);
        if (path.indexOf("://")==-1)
        {
            requestUri.append(endpoint.getProtocol()).append("://");
            requestUri.append(endpoint.getEndpointURI().getHost());
            requestUri.append(':').append(endpoint.getEndpointURI().getPort());
        }
        // first check that there is a receiver on the root address
        if (logger.isTraceEnabled())
        {
            logger.trace("Looking up receiver on connector: " + connector.getName() + " with URI key: "
                    + requestUri.toString());
        }

        UMOMessageReceiver receiver = connector.lookupReceiver(requestUri.toString());

        // If no receiver on the root and there is a request path, look up the
        // received based on the root plus request path
        if (receiver == null && !"/".equals(path))
        {
            // remove anything after the last '/'
            int x = path.lastIndexOf('/');
            if (x > 1 && path.indexOf('.') > x)
            {
                requestUri.append(path.substring(0, x));
            }
            else
            {
                requestUri.append(path);
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Secondary lookup of receiver on connector: " + connector.getName()
                        + " with URI key: " + requestUri.toString());
            }

            // try again
            String uriStr = requestUri.toString();
            receiver = connector.lookupReceiver(uriStr);
            
            if (receiver == null) 
            {
				receiver = findReceiverByStem(connector.getReceivers(), uriStr);
			}

            if (receiver == null && logger.isWarnEnabled())
            {
                logger.warn("No receiver found with secondary lookup on connector: " + connector.getName()
                        + " with URI key: " + requestUri.toString());
                logger.warn("Receivers on connector are: "
                        + MapUtils.toString(connector.getReceivers(), true));
            }
        }

        return receiver;
    }
    
    protected HttpResponse transformResponse(Object response) throws TransformerException
    {
        UMOMessage message = null;
        if(response instanceof UMOMessage)
        {
            message = (UMOMessage)response;
        }
        else
        {
            message = new MuleMessage(response);
        }
        //TODO RM*: Maybe we can have a generic Transformer wrapper rather that using MuleMessage (or another static utility 
        //class
        message.applyTransformers(connector.getDefaultResponseTransformers(), HttpResponse.class);
        return (HttpResponse) message.getPayload();
    }

    public static UMOMessageReceiver findReceiverByStem(Map receivers, String uriStr)
    {
        int match = 0;
        UMOMessageReceiver receiver = null;
        for (Iterator itr = receivers.entrySet().iterator(); itr.hasNext();)
        {
            Map.Entry e = (Map.Entry)itr.next();
            String key = (String)e.getKey();
            UMOMessageReceiver candidate = (UMOMessageReceiver)e.getValue();
            if (uriStr.startsWith(key) && match < key.length())
            {
                match = key.length();
                receiver = candidate;
            }
        }
        return receiver;
    }

}
