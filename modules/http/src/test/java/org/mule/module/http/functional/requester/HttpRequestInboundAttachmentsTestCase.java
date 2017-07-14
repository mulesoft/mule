/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.construct.Flow;
import org.mule.module.http.internal.multipart.HttpPartDataSource;
import org.mule.transport.NullPayload;

import java.io.IOException;

import javax.activation.DataHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.MultiPartWriter;
import org.junit.Test;

public class HttpRequestInboundAttachmentsTestCase extends AbstractHttpRequestTestCase
{

    private static final String FORM_DATA = "form-data";
    private static final String MIXED = "mixed";

    private String subType;

    @Override
    protected String getConfigFile()
    {
        return "http-request-inbound-attachments-config.xml";
    }

    @Test
    public void processInboundFormDataAttachments() throws Exception
    {
        subType = FORM_DATA;

        Flow flow = (Flow) getFlowConstruct("requestFlow");

        MuleEvent event = flow.process(getTestEvent(TEST_MESSAGE));

        assertThat(event.getMessage().getPayload(), is(instanceOf(NullPayload.class)));

        assertThat(event.getMessage().getInboundAttachmentNames().size(), is(2));
        assertAttachment(event.getMessage(), "partName1", "Test part 1", "text/plain");
        assertAttachment(event.getMessage(), "partName2", "Test part 2", "text/html");

        HttpPartDataSource part2Source = (HttpPartDataSource) event.getMessage().getInboundAttachment("partName2").getDataSource();
        assertThat(part2Source.getHeader("Custom-Header"), equalTo("custom"));
    }

    @Test
    public void processInboundMixedAttachments() throws Exception
    {
        subType = MIXED;

        Flow flow = (Flow) getFlowConstruct("requestFlow");

        MuleEvent event = flow.process(getTestEvent(TEST_MESSAGE));

        assertThat(event.getMessage().getPayload(), is(instanceOf(NullPayload.class)));

        assertThat(event.getMessage().getInboundAttachmentNames().size(), is(2));
        assertAttachment(event.getMessage(), "mule_attachment_0", "Test part 1", "text/plain");
        assertAttachment(event.getMessage(), "mule_attachment_1", "Test part 2", "text/html");

        HttpPartDataSource part2Source = (HttpPartDataSource) event.getMessage().getInboundAttachment("mule_attachment_1").getDataSource();
        assertThat(part2Source.getHeader("Custom-Header"), equalTo("custom"));
    }

    private void assertAttachment(MuleMessage message, String attachmentName, String attachmentContents, String contentType) throws IOException
    {
        assertTrue(message.getInboundAttachmentNames().contains(attachmentName));

        DataHandler handler = message.getInboundAttachment(attachmentName);
        assertThat(handler.getContentType(), equalTo(contentType));

        assertThat(IOUtils.toString(handler.getInputStream()), equalTo(attachmentContents));
    }

    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        MultiPartWriter multiPartWriter = new MultiPartWriter(response.getWriter());

        response.setContentType(format("multipart/%s; boundary=%s", subType, multiPartWriter.getBoundary()));
        response.setStatus(HttpServletResponse.SC_OK);

        String[] firstPartHeaders;
        String[] secondPartHeaders;

        if (FORM_DATA.equals(subType))
        {
            firstPartHeaders = new String[] { "Content-Disposition: form-data; name=\"partName1\"" };
            secondPartHeaders = new String[] {
              "Content-Disposition: form-data; name=\"partName2\"",
              "Custom-Header: custom"
            };
        }
        else
        {
            firstPartHeaders = new String[] {};
            secondPartHeaders = new String[] { "Custom-Header: custom" };
        }

        multiPartWriter.startPart("text/plain", firstPartHeaders);
        multiPartWriter.write("Test part 1");
        multiPartWriter.endPart();

        multiPartWriter.startPart("text/html", secondPartHeaders);
        multiPartWriter.write("Test part 2");
        multiPartWriter.endPart();

        multiPartWriter.close();

    }
}
