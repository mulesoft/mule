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
import static org.mule.functional.junit4.matchers.MessageMatchers.hasMediaType;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.functional.junit4.matchers.MultiPartPayloadMatchers.hasPartThat;
import static org.mule.functional.junit4.matchers.MultiPartPayloadMatchers.hasSize;
import static org.mule.functional.junit4.matchers.PartAttributesMatchers.hasName;
import static org.mule.runtime.api.metadata.MediaType.HTML;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.service.http.api.HttpHeaders;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.MultiPartWriter;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;

@Features(HTTP_EXTENSION)
public class HttpRequestInboundPartsTestCase extends AbstractHttpRequestTestCase {

  @Override
  protected String getConfigFile() {
    return "http-request-inbound-attachments-config.xml";
  }

  @Test
  public void processInboundAttachments() throws Exception {
    Event event = flowRunner("requestFlow").withPayload(TEST_MESSAGE).run();

    assertThat(event.getMessage().getPayload().getValue(), instanceOf(MultiPartPayload.class));

    MultiPartPayload payload = (MultiPartPayload) event.getMessage().getPayload().getValue();
    assertThat(payload, hasSize(2));
    assertAttachment(payload, "partName1", "Test part 1", TEXT);
    assertAttachment(payload, "partName2", "Test part 2", HTML);
  }

  private void assertAttachment(MultiPartPayload payload, String attachmentName, String attachmentContents, MediaType contentType)
      throws IOException {
    assertThat(payload, hasPartThat(hasName(attachmentName)));

    Message part = payload.getPart(attachmentName);
    assertThat(part, hasMediaType(contentType));
    assertThat(part, hasPayload(equalTo(attachmentContents)));
  }

  @Override
  protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
    MultiPartWriter multiPartWriter = new MultiPartWriter(response.getWriter());

    response.setContentType(HttpHeaders.Values.MULTIPART_FORM_DATA + "; boundary=" + multiPartWriter.getBoundary());
    response.setStatus(SC_OK);

    multiPartWriter.startPart(TEXT.toRfcString(), new String[] {"Content-Disposition: form-data; name=\"partName1\""});
    multiPartWriter.write("Test part 1");
    multiPartWriter.endPart();

    multiPartWriter.startPart(HTML.toRfcString(), new String[] {"Content-Disposition: form-data; name=\"partName2\""});
    multiPartWriter.write("Test part 2");
    multiPartWriter.endPart();

    multiPartWriter.close();
  }
}
