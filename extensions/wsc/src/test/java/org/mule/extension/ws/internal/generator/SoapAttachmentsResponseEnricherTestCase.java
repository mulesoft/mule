/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.generator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mule.extension.ws.internal.connection.WscClient.MULE_ATTACHMENTS_KEY;
import org.mule.extension.ws.internal.generator.attachment.AttachmentResponseEnricher;
import org.mule.extension.ws.internal.generator.attachment.SoapAttachmentResponseEnricher;
import org.mule.runtime.api.message.Message;

import java.util.List;

import org.apache.cxf.message.Exchange;

public class SoapAttachmentsResponseEnricherTestCase extends ResponseEnricherTestCase {

  private static final String RESPONSE =
      "<con:downloadAttachmentResponse xmlns:con=\"http://service.ws.extension.mule.org/\">"
          + "<attachment>U29tZSBDb250ZW50</attachment>"
          + "</con:downloadAttachmentResponse>";

  @Override
  protected String getResponse() {
    return RESPONSE;
  }

  @Override
  protected AttachmentResponseEnricher getEnricher() {
    return new SoapAttachmentResponseEnricher();
  }

  @Override
  protected void assertAttachment(Exchange exchange) {
    List<Message> attachments = (List<Message>) exchange.get(MULE_ATTACHMENTS_KEY);
    assertThat(attachments, hasSize(1));
    String value = (String) attachments.get(0).getPayload().getValue();
    assertThat(value, is("Some Content"));
  }
}
