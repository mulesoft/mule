/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.runtime;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mule.extension.ws.WscTestUtils.DOWNLOAD_ATT;
import static org.mule.extension.ws.WscTestUtils.SIMPLE_ATTACHMENT;
import static org.mule.extension.ws.WscTestUtils.UPLOAD_SINGLE_ATT;
import static org.mule.extension.ws.WscTestUtils.assertSimilarXml;
import static org.mule.extension.ws.WscTestUtils.getRequestResource;
import static org.mule.extension.ws.WscTestUtils.getResponseResource;
import static org.mule.extension.ws.WscTestUtils.resourceAsString;
import org.mule.extension.ws.AbstractSoapServiceTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Step;

public class MtomAttachmentsTestCase extends AbstractSoapServiceTestCase {

  private static final String DOWNLOAD_FLOW = "download";
  private static final String UPLOAD_ATTACHMENT = "upload";

  @Override
  protected String getConfigFile() {
    return "config/attachments.xml";
  }

  @Test
  @Description("Uploads an attachment to the server")
  public void uploadAttachment() throws Exception {
    String payload = getRequestResource(UPLOAD_SINGLE_ATT);
    Message message = flowRunner(UPLOAD_ATTACHMENT).withPayload(payload).withVariable("att", "Some Content").run().getMessage();
    assertSimilarXml((String) message.getPayload().getValue(), getResponseResource(UPLOAD_SINGLE_ATT));
  }

  @Test
  @Description("Downloads an attachment from the server")
  public void downloadAttachment() throws Exception {
    String payload = getRequestResource(DOWNLOAD_ATT);
    Message message = flowRunner(DOWNLOAD_FLOW).withPayload(payload).run().getMessage();
    MultiPartPayload multipart = (MultiPartPayload) message.getPayload().getValue();

    List<Message> parts = multipart.getParts();
    assertThat(parts, hasSize(2));
    Message bodyPart = parts.get(0);
    Message attachmentPart = parts.get(1);

    assertDownloadedAttachment(attachmentPart);
    assertDownloadedAttachmentBody(bodyPart, attachmentPart);
  }

  @Step("Checks that the response body is correct and references the correct attachment")
  private void assertDownloadedAttachmentBody(Message bodyPart, Message attachmentPart) throws Exception {
    // We need to format the expected response with the content id of the attachment.
    String name = ((PartAttributes) attachmentPart.getAttributes()).getName();
    String responseResource = format(getResponseResource(DOWNLOAD_ATT), name);

    assertSimilarXml((String) bodyPart.getPayload().getValue(), responseResource);
  }

  @Step("Checks that the content of the downloaded attachment is correct")
  private void assertDownloadedAttachment(Message attachmentPart) throws XMLStreamException, IOException {
    String expectedAttachmentContent = resourceAsString(SIMPLE_ATTACHMENT);
    assertThat(IOUtils.toString((InputStream) attachmentPart.getPayload().getValue()), is(expectedAttachmentContent));
  }
}
