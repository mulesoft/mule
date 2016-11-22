/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.generator;

import static java.util.Collections.singletonMap;
import static org.mule.extension.ws.WscTestUtils.UPLOAD_ATTACHMENT;
import static org.mule.extension.ws.WscTestUtils.assertSimilarXml;
import static org.mule.extension.ws.WscTestUtils.getRequestResource;
import static org.mule.extension.ws.WscTestUtils.getTestAttachment;
import org.mule.extension.ws.WscUnitTestCase;
import org.mule.extension.ws.api.SoapAttachment;
import org.mule.extension.ws.internal.generator.attachment.AttachmentRequestEnricher;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;

public abstract class AbstractRequestEnricherTestCase extends WscUnitTestCase {

  @Test
  @Description("Enrich a request that contains attachments")
  public void enrich() throws Exception {
    SoapAttachment attachment = getTestAttachment();
    String providedBody = getRequestResource(UPLOAD_ATTACHMENT);
    AttachmentRequestEnricher enricher = getEnricher();
    String request = enricher.enrichRequest(providedBody, singletonMap("attachment", attachment));
    assertSimilarXml(getExpectedResult(), request);
  }

  protected abstract AttachmentRequestEnricher getEnricher();

  protected abstract String getExpectedResult();

}
