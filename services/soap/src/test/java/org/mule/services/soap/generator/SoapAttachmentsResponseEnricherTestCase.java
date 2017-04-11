/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.generator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mule.services.soap.client.SoapCxfClient.MULE_ATTACHMENTS_KEY;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.extension.api.soap.SoapAttachment;
import org.mule.services.soap.generator.attachment.AttachmentResponseEnricher;
import org.mule.services.soap.generator.attachment.SoapAttachmentResponseEnricher;

import java.util.Map;

import org.apache.cxf.message.Exchange;

public class SoapAttachmentsResponseEnricherTestCase extends ResponseEnricherTestCase {

  private static final String RESPONSE =
      "<con:downloadAttachmentResponse xmlns:con=\"http://service.soap.services.mule.org/\">"
          + "<attachment>U29tZSBDb250ZW50</attachment>"
          + "</con:downloadAttachmentResponse>";

  @Override
  protected String getResponse() {
    return RESPONSE;
  }

  @Override
  protected AttachmentResponseEnricher getEnricher() {
    return new SoapAttachmentResponseEnricher(introspecter, loader);
  }

  @Override
  protected void assertAttachment(Exchange exchange) {
    Map<String, SoapAttachment> attachments = (Map<String, SoapAttachment>) exchange.get(MULE_ATTACHMENTS_KEY);
    assertThat(attachments.entrySet(), hasSize(1));
    String value = IOUtils.toString(attachments.get("attachment").getContent());
    assertThat(value, is("Some Content"));
  }
}
