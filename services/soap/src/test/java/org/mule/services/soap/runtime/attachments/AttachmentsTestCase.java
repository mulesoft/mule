/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.runtime.attachments;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.services.soap.api.message.SoapRequest.builder;
import org.mule.services.soap.api.message.ImmutableSoapAttachment;
import org.mule.services.soap.api.message.SoapRequest;
import org.mule.services.soap.api.message.SoapResponse;
import org.mule.services.soap.AbstractSoapServiceTestCase;

import java.io.ByteArrayInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;

public class AttachmentsTestCase extends AbstractSoapServiceTestCase {

  @Test
  @Description("Downloads an attachment from a mtom server")
  public void downloadAttachment() throws Exception {
    SoapRequest request = builder()
        .withContent("<con:downloadAttachment xmlns:con=\"http://service.soap.services.mule.org/\">\n"
            + "    <fileName>attachment.txt</fileName>\n"
            + "</con:downloadAttachment>")
        .withOperation("downloadAttachment")
        .build();
    SoapResponse response = client.consume(request);
    assertSimilarXml("<ns2:downloadAttachmentResponse xmlns:ns2=\"http://service.soap.services.mule.org/\"/>",
                     response.getContent());
    assertThat(response.getAttachments(), hasSize(1));
    assertThat(response.getAttachments().get(0).getId(), containsString("attachment"));
    assertThat(IOUtils.toString(response.getAttachments().get(0).getContent()), containsString("Simple Attachment Content"));
  }

  @Test
  @Description("Uploads an attachment to a mtom server")
  public void uploadAttachment() throws Exception {
    SoapRequest request = builder()
        .withAttachment(new ImmutableSoapAttachment("attachment", "text/html",
                                                    new ByteArrayInputStream("Some Content".getBytes())))
        .withContent("<con:uploadAttachment xmlns:con=\"http://service.soap.services.mule.org/\"/>")
        .withOperation("uploadAttachment")
        .build();
    SoapResponse response = client.consume(request);
    assertSimilarXml("<ns2:uploadAttachmentResponse xmlns:ns2=\"http://service.soap.services.mule.org/\">\n"
        + "    <result>Ok</result>\n"
        + "</ns2:uploadAttachmentResponse>", response.getContent());
  }
}
