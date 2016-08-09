/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.http.api.HttpHeaders;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.MultiPartWriter;
import org.junit.Test;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class HttpRequestInboundAttachmentsTestCase extends AbstractHttpRequestTestCase {

  @Override
  protected String getConfigFile() {
    return "http-request-inbound-attachments-config.xml";
  }

  @Test
  public void processInboundAttachments() throws Exception {
    MuleEvent event = flowRunner("requestFlow").withPayload(TEST_MESSAGE).run();

    assertThat(event.getMessage().getPayload(), instanceOf(MultiPartPayload.class));

    MultiPartPayload payload = event.getMessage().getPayload();
    assertThat(payload.getParts(), hasSize(2));
    assertAttachment(payload, "partName1", "Test part 1", MediaType.TEXT);
    assertAttachment(payload, "partName2", "Test part 2", MediaType.HTML);
  }

  private void assertAttachment(MultiPartPayload payload, String attachmentName, String attachmentContents, MediaType contentType)
      throws IOException {
    assertTrue(payload.getPartNames().contains(attachmentName));

    org.mule.runtime.api.message.MuleMessage attachment = payload.getPart(attachmentName);
    assertThat(attachment.getDataType().getMediaType(), equalTo(contentType));

    assertThat(IOUtils.toString((InputStream) attachment.getPayload()), equalTo(attachmentContents));
  }

  @Override
  protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
    MultiPartWriter multiPartWriter = new MultiPartWriter(response.getWriter());

    response.setContentType(HttpHeaders.Values.MULTIPART_FORM_DATA + "; boundary=" + multiPartWriter.getBoundary());
    response.setStatus(SC_OK);

    multiPartWriter.startPart(MediaType.TEXT.toRfcString(), new String[] {"Content-Disposition: form-data; name=\"partName1\""});
    multiPartWriter.write("Test part 1");
    multiPartWriter.endPart();

    multiPartWriter.startPart(MediaType.HTML.toRfcString(), new String[] {"Content-Disposition: form-data; name=\"partName2\""});
    multiPartWriter.write("Test part 2");
    multiPartWriter.endPart();

    multiPartWriter.close();
  }
}
