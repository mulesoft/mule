/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;

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
public class HttpServerConnection
{
    private static final Log logger = LogFactory.getLog(HttpServerConnection.class);

    private Socket socket;
    private final InputStream in;
    private final OutputStream out;
    private boolean keepAlive = false;
    private final String encoding;

    public HttpServerConnection(final Socket socket, String encoding) throws IOException
    {
        super();

        if (socket == null)
        {
            throw new IllegalArgumentException("Socket may not be null");
        }

        this.socket = socket;
        this.socket.setTcpNoDelay(true);
        this.in = socket.getInputStream();
        this.out = new DataOutputStream(socket.getOutputStream());
        this.encoding = encoding;
    }

    public void close()
    {
        try
        {
            if (socket != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Closing: " + socket);
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
        try
        {
            String line = readLine();
            if (line == null)
            {
                return null;
            }
            return new HttpRequest(RequestLine.parseLine(line), HttpParser.parseHeaders(this.in, encoding), this.in);
        }
        catch (IOException e)
        {
            close();
            throw e;
        }
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
        if (response == null)
        {
            return;
        }

        setKeepAlive(response.isKeepAlive());
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

        InputStream content = response.getBody();
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

            IOUtils.copy(content, outstream);

            if (outstream instanceof ChunkedOutputStream)
            {
                ((ChunkedOutputStream) outstream).finish();
            }
        }

        outstream.flush();
    }

    public int getSocketTimeout() throws SocketException
    {
        return this.socket.getSoTimeout();
    }

    public void setSocketTimeout(int timeout) throws SocketException
    {
        this.socket.setSoTimeout(timeout);
    }
}
