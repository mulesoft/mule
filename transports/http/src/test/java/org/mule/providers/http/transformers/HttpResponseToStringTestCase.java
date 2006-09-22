/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.transformers;

import junit.framework.TestCase;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpVersion;
import org.mule.providers.http.HttpConstants;
import org.mule.providers.http.HttpResponse;
import org.mule.providers.http.ResponseWriter;
import org.mule.umo.transformer.TransformerException;

/**
 * Test case for HttpResponseToString transformer
 * 
 * @author <a href="mailto:mlusetti@gmail.com">Massimo Lusetti</a>
 */
public class HttpResponseToStringTestCase extends TestCase
{
    private final String _statusLine = "HTTP/1.1 200 OK";
    private final String _headerCT = "Content-Type: text/plain";
    private final String _headerTE = "Transfer-Encoding: chunked";
    private final String _contentLength = "Content-Length: ";
    private final String _body = "<html><head></head><body><p>WOW</p></body></html>";

    private String _resultChunked = _statusLine + ResponseWriter.CRLF + _headerCT + ResponseWriter.CRLF
                                    + _contentLength + _body.length() + ResponseWriter.CRLF + _headerTE
                                    + ResponseWriter.CRLF + ResponseWriter.CRLF;
    private String _resultNotChunked = _statusLine + ResponseWriter.CRLF + _headerCT + ResponseWriter.CRLF
                                       + _contentLength + _body.length() + ResponseWriter.CRLF
                                       + ResponseWriter.CRLF;

    private HttpResponse _resp = null;

    protected void setUp() throws Exception
    {
        super.setUp();

        _resp = new HttpResponse();
        _resp.setStatusLine(new HttpVersion(1, 1), 200);
        _resp.setHeader(new Header(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.DEFAULT_CONTENT_TYPE));
        _resp.setBodyString(_body);
    }

    /**
     * Check consistency of the transformed {@link HttpResponse} string when HTTP
     * transfer encoding is chunked
     * 
     * @throws Exception
     */
    public void testTransformChunked() throws Exception
    {
        HttpResponseToString trasf = new HttpResponseToString();
        trasf.setReturnClass(String.class);

        _resp.setHeader(new Header(HttpConstants.HEADER_TRANSFER_ENCODING,
            HttpConstants.TRANSFER_ENCODING_CHUNKED));
        _resultChunked += "31\r\n" + _body + "\r\n0\r\n\r\n";

        String trasfRes = (String)trasf.doTransform(_resp, "ISO-8859-1");

        assertEquals(_resultChunked, trasfRes);
    }

    /**
     * Check consistency of the transformed {@link HttpResponse} string when HTTP
     * transfer encoding is chunked
     * 
     * @throws Exception
     */
    public void testTransformNotChunked() throws Exception
    {
        HttpResponseToString trasf = new HttpResponseToString();
        trasf.setReturnClass(String.class);

        _resultNotChunked += _body;

        String trasfRes = (String)trasf.doTransform(_resp, "ISO-8859-1");

        assertEquals(_resultNotChunked, trasfRes);
    }

    /**
     * Expect a {@link TransformerException} when the encoding is not supported.
     */
    public void testTransformException()
    {
        try
        {
            HttpResponseToString trasf = new HttpResponseToString();
            trasf.doTransform(_resp, "ISO-8859-20");

            fail();
        }
        catch (TransformerException tfe)
        {
            // Expected
        }
    }

}
