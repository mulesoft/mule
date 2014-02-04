/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import org.mule.transport.http.HttpRequest;
import org.mule.transport.http.RequestLine;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.httpclient.HttpParser;

public abstract class SingleRequestMockHttpServer extends MockHttpServer
{

    private final String encoding;
    private String statusLine;

    public SingleRequestMockHttpServer(int listenPort, String encoding)
    {
        this(listenPort, encoding, HTTP_STATUS_LINE_OK);
    }

    public SingleRequestMockHttpServer(int listenPort, String encoding, String statusLine)
    {
        super(listenPort);
        this.encoding = encoding;
        this.statusLine = statusLine;
    }

    protected abstract void processSingleRequest(HttpRequest httpRequest) throws Exception;

    @Override
    protected void processRequests(InputStream in, OutputStream out) throws Exception
    {
        String line = HttpParser.readLine(in, encoding);
        RequestLine requestLine = RequestLine.parseLine(line);
        HttpRequest request = new HttpRequest(requestLine, HttpParser.parseHeaders(in, encoding), in, encoding);

        processSingleRequest(request);

        out.write(statusLine.getBytes());
        out.write('\n');
        out.flush();
    }

}


