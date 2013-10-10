/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HttpMultipartMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{

    private static final String REQUEST_LINE = "POST /services/Echo HTTP/1.1";
    private static final String MULTIPART_BOUNDARY = "----------------------------299df9f9431b";
    private static final Header[] HEADERS = new Header[]{new Header("Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY)};
    private static final String MULTIPART_MESSAGE = "--" + MULTIPART_BOUNDARY + "\r\n"
                                                    + "Content-Disposition: form-data; name=\"payload\"; filename=\"payload\"\r\n"
                                                    + "Content-Type: application/octet-stream\r\n\r\n" +
                                                    "part payload\r\n\r\n" +
                                                    "--" + MULTIPART_BOUNDARY + "\r\n"
                                                    + "Content-Disposition: form-data; name=\"two\"; filename=\"two\"\r\n"
                                                    + "Content-Type: application/octet-stream\r\n\r\n" + "part two\r\n\r\n" +
                                                    "--" + MULTIPART_BOUNDARY + "--\r\n\r\n";

    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new HttpMultipartMuleMessageFactory(muleContext);
    }

    @Override
    protected Object getValidTransportMessage() throws Exception
    {
        RequestLine requestLine = RequestLine.parseLine(REQUEST_LINE);
        HttpRequest request = new HttpRequest(requestLine, HEADERS, null, encoding);
        return request;
    }

    @Override
    protected Object getUnsupportedTransportMessage()
    {
        return "this is not a valid transport message for HttpMuleMessageFactory";
    }

    @Override
    @Test
    public void testValidPayload() throws Exception
    {
        HttpMuleMessageFactory factory = (HttpMuleMessageFactory) createMuleMessageFactory();
        factory.setExchangePattern(MessageExchangePattern.ONE_WAY);
        HttpRequest request = createMultiPartHttpRequest();
        MuleMessage message = factory.create(request, encoding);
        assertNotNull(message);
        assertTrue(message.getPayload() instanceof InputStream);
    }

    private HttpRequest createMultiPartHttpRequest() throws Exception
    {
        RequestLine requestLine = RequestLine.parseLine(REQUEST_LINE);
        InputStream stream = new ByteArrayInputStream(MULTIPART_MESSAGE.getBytes());
        return new HttpRequest(requestLine, HEADERS, stream, encoding);
    }

}


