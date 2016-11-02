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
import static org.mule.extension.ws.WscTestUtils.DOWNLOAD_ATTACHMENT;
import static org.mule.extension.ws.WscTestUtils.UPLOAD_ATTACHMENT;
import static org.mule.extension.ws.WscTestUtils.assertSimilarXml;
import static org.mule.extension.ws.WscTestUtils.getRequestResource;
import static org.mule.extension.ws.WscTestUtils.getResponseResource;
import static org.mule.extension.ws.WscTestUtils.getTestAttachment;
import org.mule.extension.ws.AbstractSoapServiceTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Step;

public abstract class AttachmentsTestCase extends AbstractSoapServiceTestCase {

  @Rule
  public SystemProperty mtom;

  public AttachmentsTestCase(Boolean isMtom) {
    mtom = new SystemProperty("mtomEnabled", isMtom.toString());
  }

  @Override
  protected String getConfigFile() {
    return "config/attachments.xml";
  }

  @Test
  @Description("Uploads an attachment to the server")
  public void uploadAttachment() throws Exception {
    String payload = getRequestResource(UPLOAD_ATTACHMENT);
    Message message =
        flowRunner(UPLOAD_ATTACHMENT).withPayload(payload).withVariable("inAttachment", getTestAttachment()).run().getMessage();
    assertSimilarXml((String) message.getPayload().getValue(), getResponseResource(UPLOAD_ATTACHMENT));
  }

  @Test
  @Description("Downloads an attachment from the server")
  public void downloadAttachment() throws Exception {
    String payload = getRequestResource(DOWNLOAD_ATTACHMENT);
    Message message = flowRunner(DOWNLOAD_ATTACHMENT).withPayload(payload).run().getMessage();
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
    String responseResource = format(getResponseResource(DOWNLOAD_ATTACHMENT), name);
    assertSimilarXml((String) bodyPart.getPayload().getValue(), responseResource);
  }

  @Step("Checks that the content of the downloaded attachment is correct")
  protected abstract void assertDownloadedAttachment(Message attachmentPart) throws XMLStreamException, IOException;
}
