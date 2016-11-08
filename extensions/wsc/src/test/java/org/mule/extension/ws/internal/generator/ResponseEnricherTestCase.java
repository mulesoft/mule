/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.generator;

import static org.mule.extension.ws.WscTestUtils.DOWNLOAD_ATTACHMENT;
import static org.mule.extension.ws.WscTestUtils.assertSimilarXml;
import static org.mule.extension.ws.WscTestUtils.getResponseResource;
import static org.mule.extension.ws.internal.util.TransformationUtils.stringToDocument;
import org.mule.extension.ws.AbstracWscUnitTestCase;
import org.mule.extension.ws.internal.generator.attachment.AttachmentResponseEnricher;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.junit.Test;
import org.w3c.dom.Document;
import ru.yandex.qatools.allure.annotations.Description;

abstract class ResponseEnricherTestCase extends AbstracWscUnitTestCase {

  @Test
  @Description("Enrich a response that contains attachments")
  public void enrich() throws Exception {
    ExchangeImpl exchange = new ExchangeImpl();
    Document doc = stringToDocument(getResponse());
    AttachmentResponseEnricher enricher = getEnricher();
    String result = enricher.enrich(doc, attachmentsServiceIntrospecter, attachmentsTypeLoader, DOWNLOAD_ATTACHMENT, exchange);
    assertSimilarXml(getResponseResource(DOWNLOAD_ATTACHMENT), result);
    assertAttachment(exchange);
  }

  protected abstract AttachmentResponseEnricher getEnricher();

  protected abstract String getResponse();

  protected abstract void assertAttachment(Exchange exchange);
}
