/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.core.api.MuleEvent;

import java.io.IOException;
import java.util.Map;

import javax.activation.DataHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.MultiPartWriter;
import org.junit.Test;

public class HttpRequestInboundAttachmentsTestCase extends AbstractHttpRequestTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "http-request-inbound-attachments-config.xml";
    }

    @Test
    public void processInboundAttachments() throws Exception
    {
        MuleEvent event = flowRunner("requestFlow").withPayload(TEST_MESSAGE).run();

        assertThat(event.getMessage().getPayload(), is(instanceOf(NullPayload.class)));

        Map<String, DataHandler> parts = ((HttpResponseAttributes) event.getMessage().getAttributes()).getParts();
        assertThat(parts.size(), is(2));
        assertAttachment(parts, "partName1", "Test part 1", "text/plain");
        assertAttachment(parts, "partName2", "Test part 2", "text/html");
    }

    private void assertAttachment(Map<String, DataHandler> parts, String attachmentName, String attachmentContents, String contentType) throws IOException
    {
        assertTrue(parts.keySet().contains(attachmentName));

        DataHandler handler = parts.get(attachmentName);
        assertThat(handler.getContentType(), equalTo(contentType));

        assertThat(IOUtils.toString(handler.getInputStream()), equalTo(attachmentContents));
    }

    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        MultiPartWriter multiPartWriter = new MultiPartWriter(response.getWriter());

        response.setContentType("multipart/form-data; boundary=" + multiPartWriter.getBoundary());
        response.setStatus(HttpServletResponse.SC_OK);

        multiPartWriter.startPart("text/plain", new String[] { "Content-Disposition: form-data; name=\"partName1\"" });
        multiPartWriter.write("Test part 1");
        multiPartWriter.endPart();

        multiPartWriter.startPart("text/html", new String[] { "Content-Disposition: form-data; name=\"partName2\"" });
        multiPartWriter.write("Test part 2");
        multiPartWriter.endPart();

        multiPartWriter.close();
    }
}
