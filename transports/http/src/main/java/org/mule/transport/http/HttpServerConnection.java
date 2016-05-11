/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import org.mule.RequestContext;
import org.mule.api.transport.Connector;
import org.mule.api.transport.OutputHandler;
import org.mule.util.SystemUtils;
import org.mule.util.concurrent.Latch;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;

import org.apache.commons.httpclient.ChunkedOutputStream;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpParser;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A connection to the SimpleHttpServer.
 */
public class HttpServerConnection implements HandshakeCompletedListener
{

    private static final Log logger = LogFactory.getLog(HttpServerConnection.class);

    private Socket socket;
    private final InputStream in;
    private final OutputStream out;
    // this should rather be isKeepSocketOpen as this is the main purpose of this flag
    private boolean keepAlive = false;
    private final String encoding;
    private HttpRequest cachedRequest;
    private Latch sslSocketHandshakeComplete = new Latch();
    private Certificate[] peerCertificateChain;
    private Certificate[] localCertificateChain;
    private RequestLine requestLine;

    public HttpServerConnection(final Socket socket, String encoding, HttpConnector connector) throws IOException
    {
        super();

        if (socket == null)
        {
            throw new IllegalArgumentException("Socket may not be null");
        }

        this.socket = socket;

        if (this.socket instanceof SSLSocket)
        {
            ((SSLSocket) socket).addHandshakeCompletedListener(this);
        }

        setSocketTcpNoDelay(connector.isSendTcpNoDelay());
        this.socket.setKeepAlive(connector.isKeepAlive());

        if (connector.getReceiveBufferSize() != Connector.INT_VALUE_NOT_SET
            && socket.getReceiveBufferSize() != connector.getReceiveBufferSize())
        {
            socket.setReceiveBufferSize(connector.getReceiveBufferSize());
        }
        if (connector.getServerSoTimeout() != Connector.INT_VALUE_NOT_SET
            && socket.getSoTimeout() != connector.getServerSoTimeout())
        {
            socket.setSoTimeout(connector.getServerSoTimeout());
        }

        this.in = new BufferedInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.encoding = encoding;
    }

    private void setSocketTcpNoDelay(boolean tcpNoDelay) throws IOException
    {
        try
        {
            socket.setTcpNoDelay(tcpNoDelay);
        }
        catch (SocketException se)
        {
            if (SystemUtils.IS_OS_SOLARIS || SystemUtils.IS_OS_SUN_OS)
            {
                // this is a known Solaris bug, see
                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6378870

                if (logger.isDebugEnabled())
                {
                    logger.debug("Failed to set tcpNoDelay on socket", se);
                }
            }
            else
            {
                throw se;
            }
        }
    }

    public synchronized void close()
    {
        try
        {
            if (socket != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Closing: " + socket);
                }

                try
                {
                    socket.shutdownOutput();
                }
                catch (UnsupportedOperationException e)
                {
                    //Can't shutdown in/output on SSL sockets
                }

                if (in != null)
                {
                    in.close();
                }
                if (out != null)
                {
                    out.close();
                }
                socket.close();
            }
        }
        catch (IOException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("(Ignored) Error closing the socket: " + e.getMessage());
            }
        }
        finally
        {
            socket = null;
        }
    }

    public synchronized boolean isOpen()
    {
        return this.socket != null;
    }

    public void setKeepAlive(boolean b)
    {
        this.keepAlive = b;
    }

    public boolean isKeepAlive()
    {
        return this.keepAlive;
    }

    public InputStream getInputStream()
    {
        return this.in;
    }

    public OutputStream getOutputStream()
    {
        return this.out;
    }

    /**
     * Returns the ResponseWriter used to write the output to the socket.
     *
     * @return This connection's ResponseWriter
     */
    public ResponseWriter getWriter() throws UnsupportedEncodingException
    {
        return new ResponseWriter(out);
    }

    public HttpRequest readRequest() throws IOException
    {
        if (cachedRequest != null)
        {
            return cachedRequest;
        }
        try
        {
            cachedRequest = createHttpRequest();
            return cachedRequest;
        }
        catch (IOException e)
        {
            close();
            throw e;
        }
    }

    protected HttpRequest createHttpRequest() throws IOException
    {
        return new HttpRequest(getRequestLine(), HttpParser.parseHeaders(this.in, encoding), this.in, encoding);
    }

    public HttpResponse readResponse() throws IOException
    {
        try
        {
            String line = readLine();
            return new HttpResponse(new StatusLine(line), HttpParser.parseHeaders(this.in, encoding), this.in);
        }
        catch (IOException e)
        {
            close();
            throw e;
        }
    }

    private String readLine() throws IOException
    {
        String line;

        do
        {
            line = HttpParser.readLine(in, encoding);
        }
        while (line != null && line.length() == 0);

        if (line == null)
        {
            setKeepAlive(false);
            return null;
        }

        return line;
    }

    public void writeRequest(final HttpRequest request) throws IOException
    {
        if (request == null)
        {
            return;
        }
        ResponseWriter writer = new ResponseWriter(this.out, encoding);
        writer.println(request.getRequestLine().toString());
        Iterator item = request.getHeaderIterator();
        while (item.hasNext())
        {
            Header header = (Header) item.next();
            writer.print(header.toExternalForm());
        }
        writer.println();
        writer.flush();

        OutputStream outstream = this.out;
        InputStream content = request.getBody();
        if (content != null)
        {
            Header transferenc = request.getFirstHeader(HttpConstants.HEADER_TRANSFER_ENCODING);
            if (transferenc != null)
            {
                request.removeHeaders(HttpConstants.HEADER_CONTENT_LENGTH);
                if (transferenc.getValue().indexOf(HttpConstants.TRANSFER_ENCODING_CHUNKED) != -1)
                {
                    outstream = new ChunkedOutputStream(outstream);
                }
            }

            IOUtils.copy(content, outstream);

            if (outstream instanceof ChunkedOutputStream)
            {
                ((ChunkedOutputStream) outstream).finish();
            }
        }

        outstream.flush();
    }

    public void writeResponse(final HttpResponse response) throws IOException
    {
        writeResponse(response, new HashMap<String,String>());
    }

    /**
     * Write an HttpResponse and add the map entries as headers
     *
     * @param response http response with the content of the response
     * @param headers headers to add to the http response besides the one already contained in the HttpResponse object
     * @throws IOException
     */
    public void writeResponse(final HttpResponse response, Map<String,String> headers) throws IOException
    {
        if (response == null)
        {
            return;
        }

        if (!response.isKeepAlive())
        {
            Header header = new Header(HttpConstants.HEADER_CONNECTION, "close");
            response.setHeader(header);
        }

        setKeepAlive(response.isKeepAlive());
        addHeadersToHttpResponse(response, headers);
        ResponseWriter writer = new ResponseWriter(this.out, encoding);
        OutputStream outstream = this.out;

        writer.println(response.getStatusLine());
        Iterator item = response.getHeaderIterator();
        while (item.hasNext())
        {
            Header header = (Header) item.next();
            writer.print(header.toExternalForm());
        }
        writer.println();
        writer.flush();

        OutputHandler content = response.getBody();
        if (content != null)
        {
            Header transferenc = response.getFirstHeader(HttpConstants.HEADER_TRANSFER_ENCODING);
            if (transferenc != null)
            {
                response.removeHeaders(HttpConstants.HEADER_CONTENT_LENGTH);
                if (transferenc.getValue().indexOf(HttpConstants.TRANSFER_ENCODING_CHUNKED) != -1)
                {
                    outstream = new ChunkedOutputStream(outstream);
                }
            }

            content.write(RequestContext.getEvent(), outstream);

            if (outstream instanceof ChunkedOutputStream)
            {
                ((ChunkedOutputStream) outstream).finish();
            }
        }

        outstream.flush();
    }

    /**
     * Returns the path of the http request without the http parameters encoded in the URL
     *
     * @return
     * @throws IOException
     */
    public String getUrlWithoutRequestParams() throws IOException
    {
        return readRequest().getUrlWithoutParams();
    }

    public String getRemoteClientAddress()
    {
        final SocketAddress clientAddress = socket.getRemoteSocketAddress();
        if (clientAddress != null)
        {
            return clientAddress.toString();
        }
        return null;
    }

    /**
     * Sends to the customer a Failure response.
     *
     * @param statusCode  http status code to send to the client
     * @param description description to send as the body of the response
     * @throws IOException when it's not possible to write the response back to the client.
     */
    public void writeFailureResponse(int statusCode, String description) throws IOException
    {
        writeFailureResponse(statusCode, description, new HashMap<String,String>());
    }


    /**
     * Sends a customer a failure response but also adds the headers in the headers map
     *
     * @param statusCode status code of the failure response
     * @param description message to be send as the body
     * @param headers headers to send with the failure response
     * @throws IOException
     */
    public void writeFailureResponse(int statusCode, String description, Map<String, String> headers) throws IOException
    {
        HttpResponse response = new HttpResponse();
        response.setStatusLine(readRequest().getRequestLine().getHttpVersion(), statusCode);
        response.setBody(description);
        addHeadersToHttpResponse(response, headers);
        writeResponse(response);
    }

    private void addHeadersToHttpResponse(HttpResponse response, Map<String, String> headers)
    {
        for (String headerName : headers.keySet())
        {
            response.addHeader(new Header(headerName, headers.get(headerName)));
        }
    }

    /**
     * @return the uri for the request including scheme, host, port and path. i.e: http://192.168.1.1:7777/service/orders
     * @throws IOException
     */
    public String getFullUri() throws IOException
    {
        String scheme = "http";
        if (socket instanceof SSLSocket)
        {
            scheme = "https";
        }
        InetSocketAddress localSocketAddress = (InetSocketAddress) socket.getLocalSocketAddress();
        return String.format("%s://%s:%d%s", scheme, localSocketAddress.getHostName(), localSocketAddress.getPort(), readRequest().getUrlWithoutParams());
    }

    /**
     * Returns the value of the SO_TIMEOUT for the underlying socket.
     *
     * @return The value of the SO_TIMEOUT for the underlying socket.
     * @throws SocketException If there is an error in the underlying protocol.
     */
    public int getSocketTimeout() throws SocketException
    {
        return socket.getSoTimeout();
    }

    public void setSocketTimeout(int timeout) throws SocketException
    {
        socket.setSoTimeout(timeout);
    }

    /**
     * Tests if SO_KEEPALIVE is enabled in the underlying socket.
     *
     * @return a <code>boolean</code> indicating whether or not SO_KEEPALIVE is enabled.
     * @throws SocketException If there is an error in the underlying protocol.
     */
    public boolean isSocketKeepAlive() throws SocketException
    {
        return socket.getKeepAlive();
    }

    /**
     * Tests if TCP_NODELAY is enabled in the underlying socket.
     *
     * @return a <code>boolean</code> indicating whether or not TCP_NODELAY is enabled.
     * @throws SocketException If there is an error in the underlying protocol.
     */
    public boolean isSocketTcpNoDelay() throws SocketException
    {
        return socket.getTcpNoDelay();
    }

    /**
     * Gets the value of the SO_RCVBUF for the underlying socket.
     *
     * @return The value of the SO_RCVBUF for the underlying socket.
     * @throws SocketException If there is an error in the underlying protocol.
     */
    public int getSocketReceiveBufferSize() throws SocketException
    {
        return socket.getReceiveBufferSize();
    }

    public Latch getSslSocketHandshakeCompleteLatch()
    {
        if (!(socket instanceof SSLSocket))
        {
            throw new IllegalStateException("The socket type is not SSL");
        }
        return sslSocketHandshakeComplete;
    }

    /**
     * Clean up cached values.
     * <p/>
     * Must be called if a new request from the same socket associated with the instance is going to be processed.
     */
    public void reset() throws IOException
    {
        this.requestLine = null;
        if (this.cachedRequest != null)
        {
            if (cachedRequest.getBody() != null)
            {
                cachedRequest.getBody().close();
            }
            this.cachedRequest = null;
        }
    }

    @Override
    public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent)
    {
        try
        {
            localCertificateChain = handshakeCompletedEvent.getLocalCertificates();
            try
            {
                peerCertificateChain = handshakeCompletedEvent.getPeerCertificates();
            }
            catch (SSLPeerUnverifiedException e)
            {
                logger.debug("Cannot get peer certificate chain: " + e.getMessage());
            }
        }
        finally
        {
            sslSocketHandshakeComplete.release();
        }
    }

    public Certificate[] getLocalCertificateChain()
    {
        if (!(socket instanceof SSLSocket))
        {
            throw new IllegalStateException("The socket type is not SSL");
        }
        return localCertificateChain;
    }

    public Certificate[] getPeerCertificateChain()
    {
        if (!(socket instanceof SSLSocket))
        {
            throw new IllegalStateException("The socket type is not SSL");
        }
        return peerCertificateChain;
    }

    public RequestLine getRequestLine() throws IOException
    {
        if (this.requestLine == null)
        {
            String line = readLine();
            if (line != null)
            {
                this.requestLine = RequestLine.parseLine(line);
            }
        }
        return this.requestLine;
    }
}
