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

import org.apache.commons.httpclient.ChunkedOutputStream;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpParser;
import org.apache.commons.httpclient.StatusLine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;

/**
 * A connection to the SimpleHttpServer.
 */
public class HttpServerConnection {

    private static final String HTTP_ELEMENT_CHARSET = "US-ASCII";

    private Socket socket = null;
    private InputStream in = null;
    private OutputStream out = null;
    private boolean keepAlive = false;

    public HttpServerConnection(final Socket socket)
    throws IOException {
        super();
        if (socket == null) {
            throw new IllegalArgumentException("Socket may not be null");
        }
        this.socket = socket;
        this.socket.setTcpNoDelay(true);
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
    }

    public synchronized void close() {
        try {
            if (socket != null) {
                in.close();
                out.close();
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
        }
    }

    public synchronized boolean isOpen() {
        return this.socket != null;
    }

    public void setKeepAlive(boolean b) {
        this.keepAlive = b;
    }

    public boolean isKeepAlive() {
        return this.keepAlive;
    }

    public InputStream getInputStream() {
        return this.in;
    }

    public OutputStream getOutputStream() {
        return this.out;
    }

    /**
     * Returns the ResponseWriter used to write the output to the socket.
     *
     * @return This connection's ResponseWriter
     */
    public ResponseWriter getWriter() throws UnsupportedEncodingException {
        return new ResponseWriter(out);
    }

    public HttpRequest readRequest() throws IOException {
       String line = null;
        try {
            do {
                line = HttpParser.readLine(in, HTTP_ELEMENT_CHARSET);
            } while (line != null && line.length() == 0);

            if (line == null) {
                setKeepAlive(false);
                return null;
            }
            HttpRequest request = new HttpRequest(
                    RequestLine.parseLine(line),
                    HttpParser.parseHeaders(this.in, HTTP_ELEMENT_CHARSET),
                    this.in);
            return request;
        } catch (IOException e) {
            System.out.println(line);
            close();
            throw e;
        }
    }

    public HttpResponse readResponse() throws IOException {
        try {
            String line = null;
            do {
                line = HttpParser.readLine(in, HTTP_ELEMENT_CHARSET);
            } while (line != null && line.length() == 0);

            if (line == null) {
                setKeepAlive(false);
                return null;
            }
            HttpResponse response = new HttpResponse(
                    new StatusLine(line),
                    HttpParser.parseHeaders(this.in, HTTP_ELEMENT_CHARSET),
                    this.in);
            return response;
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    public void writeRequest(final HttpRequest request) throws IOException {
        if (request == null) {
            return;
        }
        ResponseWriter writer = new ResponseWriter(this.out, HTTP_ELEMENT_CHARSET);
        writer.println(request.getRequestLine().toString());
        Iterator item = request.getHeaderIterator();
        while (item.hasNext()) {
            Header header = (Header) item.next();
            writer.print(header.toExternalForm());
        }
        writer.println();
        writer.flush();

        OutputStream outsream = this.out;
        InputStream content = request.getBody();
        if (content != null) {

            Header transferenc = request.getFirstHeader("Transfer-Encoding");
            if (transferenc != null) {
                request.removeHeaders("Content-Length");
                if (transferenc.getValue().indexOf("chunked") != -1) {
                    outsream = new ChunkedOutputStream(outsream);
                }
            }
            byte[] tmp = new byte[4096];
            int i = 0;
            while ((i = content.read(tmp)) >= 0) {
                outsream.write(tmp, 0, i);
            }
            if (outsream instanceof ChunkedOutputStream) {
                ((ChunkedOutputStream)outsream).finish();
            }
        }
        outsream.flush();
    }

    public void writeResponse(final HttpResponse response) throws IOException {
        if (response == null) {
            return;
        }
        setKeepAlive(response.isKeepAlive());
        ResponseWriter writer = new ResponseWriter(this.out, HTTP_ELEMENT_CHARSET);
        OutputStream outsream = this.out;
//        outsream.write(response.getStatusLine().getBytes());
//        outsream.write(Utility.CRLF.getBytes());
        writer.println(response.getStatusLine());
        Iterator item = response.getHeaderIterator();
        while (item.hasNext()) {
            Header header = (Header) item.next();
//            outsream.write(header.toExternalForm().getBytes());
//            outsream.write(Utility.CRLF.getBytes());
            writer.print(header.toExternalForm());
        }
        writer.println();
        writer.flush();
        //outsream.write(Utility.CRLF.getBytes());

        InputStream content = response.getBody();
        if (content != null) {
//            outsream.write("Howdie".getBytes());
//            outsream.flush();
            Header transferenc = response.getFirstHeader("Transfer-Encoding");
            if (transferenc != null) {
                response.removeHeaders("Content-Length");
                if (transferenc.getValue().indexOf("chunked") != -1) {
                    outsream = new ChunkedOutputStream(outsream);

                    byte[] tmp = new byte[1024];
                    int i = 0;
                    while ((i = content.read(tmp)) >= 0) {
                        //System.out.println(new String(tmp, 0, i));
                        outsream.write(tmp, 0, i);
                    }
                    if (outsream instanceof ChunkedOutputStream) {
                        ((ChunkedOutputStream) outsream).finish();
                    }
                }
            } else {
                /**
                 * read the content when needed to embed content-length
                 */
                byte[] tmp = new byte[1024];
                int i = 0;
                while ((i = content.read(tmp)) >= 0) {
                    outsream.write(tmp, 0, i);
                }

            }
        }
        outsream.flush();
    }

    public int getSocketTimeout() throws SocketException {
        return this.socket.getSoTimeout();
    }

    public void setSocketTimeout(int timeout) throws SocketException {
        this.socket.setSoTimeout(timeout);
    }
}
