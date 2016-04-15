/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_DISPOSITION;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.core.transformer.types.MimeTypes.HTML;
import static org.mule.runtime.core.transformer.types.MimeTypes.TEXT;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.message.ds.ByteArrayDataSource;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.runtime.core.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.MultiPartInputStreamParser;
import org.junit.Rule;
import org.junit.Test;

public class HttpRequestOutboundAttachmentsTestCase extends AbstractHttpRequestTestCase
{
    private static final String TEST_FILE_NAME = "auth/realm.properties";
    private static final String TEST_PART_NAME = "partName";

    @Rule
    public SystemProperty sendBufferSize = new SystemProperty("sendBufferSize", "128");

    @Override
    protected String getConfigFile()
    {
        return "http-request-outbound-attachments-config.xml";
    }

    private Collection<Part> parts;
    private String requestContentType;

    @Override
    protected boolean enableHttps()
    {
        return true;
    }

    @Test
    public void payloadIsIgnoredWhenSendingOutboundAttachments() throws Exception
    {
        flowRunner("requestFlow").withPayload(TEST_MESSAGE)
                                 .withOutboundAttachment("attachment1", "Contents 1", TEXT)
                                 .withOutboundAttachment("attachment2", "Contents 2", HTML)
                                 .run();

        assertThat(requestContentType, startsWith("multipart/form-data; boundary="));
        assertThat(parts.size(), equalTo(2));

        assertPart("attachment1", TEXT, "Contents 1");
        assertPart("attachment2", HTML, "Contents 2");
    }

    @Test
    public void outboundAttachmentsCustomContentType() throws Exception
    {
        flowRunner("requestFlow").withPayload(TEST_MESSAGE)
                                 .withOutboundAttachment("attachment1", "Contents 1", TEXT)
                                 .withOutboundAttachment("attachment2", "Contents 2", HTML)
                                 .withOutboundProperty("Content-Type", "multipart/form-data2")
                                 .run();

        assertThat(requestContentType, startsWith("multipart/form-data2; boundary="));
        assertThat(parts.size(), equalTo(2));

        assertPart("attachment1", TEXT, "Contents 1");
        assertPart("attachment2", HTML, "Contents 2");
    }

    @Test
    public void fileOutboundAttachmentSetsContentDispositionWithFileName() throws Exception
    {
        File file = new File(IOUtils.getResourceAsUrl(TEST_FILE_NAME, getClass()).getPath());
        DataHandler dataHandler = new DataHandler(new FileDataSource(file));

        flowRunner("requestFlow").withPayload(TEST_MESSAGE)
                                 .withOutboundAttachment(TEST_PART_NAME, dataHandler)
                                 .run();

        Part part = getPart(TEST_PART_NAME);
        assertFormDataContentDisposition(part, TEST_PART_NAME, TEST_FILE_NAME.substring(5));
    }

    @Test
    public void byteArrayOutboundAttachmentSetsContentDispositionWithFileName() throws Exception
    {
        DataHandler dataHandler = new DataHandler(new ByteArrayDataSource(TEST_MESSAGE.getBytes(), TEXT, TEST_FILE_NAME));

        flowRunner("requestFlow").withPayload(TEST_MESSAGE)
                                 .withOutboundAttachment(TEST_PART_NAME, dataHandler)
                                 .run();

        Part part = getPart(TEST_PART_NAME);
        assertFormDataContentDisposition(part, TEST_PART_NAME, TEST_FILE_NAME);
    }

    @Test
    public void stringOutboundAttachmentSetsContentDispositionWithoutFileName() throws Exception
    {
        flowRunner("requestFlow").withPayload(TEST_MESSAGE)
                                 .withOutboundAttachment(TEST_PART_NAME, TEST_MESSAGE, TEXT)
                                 .run();

        Part part = getPart(TEST_PART_NAME);
        assertFormDataContentDisposition(part, TEST_PART_NAME, null);
    }

    @Test
    public void sendingAttachmentBiggerThanAsyncWriteQueueSizeWorksOverHttps() throws Exception
    {
        // Grizzly defines the maxAsyncWriteQueueSize as 4 times the sendBufferSize (org.glassfish.grizzly.nio.transport.TCPNIOConnection).
        int maxAsyncWriteQueueSize = Integer.valueOf(sendBufferSize.getValue()) * 4;

        MuleEvent response = flowRunner("requestFlowTls").withPayload(TEST_MESSAGE)
                                                         // Set an attachment bigger than the queue size.
                                                         .withOutboundAttachment(TEST_PART_NAME, new byte[maxAsyncWriteQueueSize * 2], TEXT)
                                                         .run();
        
        assertThat((Integer) response.getMessage().getInboundProperty(HTTP_STATUS_PROPERTY), equalTo(OK.getStatusCode()));
    }

    private void assertPart(String name, String expectedContentType, String expectedBody) throws Exception
    {
        Part part = getPart(name);
        assertThat(part, notNullValue());
        assertThat(part.getContentType(), startsWith(expectedContentType));
        assertThat(IOUtils.toString(part.getInputStream()), equalTo(expectedBody));
    }

    private void assertFormDataContentDisposition(Part part, String expectedName, String expectedFileName)
    {
        String expected = String.format("form-data; name=\"%s\"", expectedName);
        if (expectedFileName != null)
        {
            expected += String.format("; filename=\"%s\"", expectedFileName);
        }

        assertThat(part.getHeader(CONTENT_DISPOSITION), equalTo(expected));
    }

    private Part getPart(String name)
    {
        for (Part part : parts)
        {
            if (part.getName().equals(name))
            {
                return part;
            }
        }
        return null;
    }

    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        requestContentType = request.getHeader(CONTENT_TYPE);

        MultiPartInputStreamParser inputStreamParser = new MultiPartInputStreamParser(request.getInputStream(), request.getContentType(), null, null);

        try
        {
            parts = inputStreamParser.getParts();
        }
        catch (ServletException e)
        {
            throw new IOException(e);
        }


        response.setContentType(HTML);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print(DEFAULT_RESPONSE);
    }
}
