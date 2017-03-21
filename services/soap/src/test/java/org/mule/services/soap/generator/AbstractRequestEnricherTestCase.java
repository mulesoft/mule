/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.generator;

import static java.util.Arrays.asList;
import org.mule.services.soap.SoapTestUtils;
import org.mule.services.soap.api.message.SoapAttachment;
import org.mule.services.soap.generator.attachment.AttachmentRequestEnricher;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;

public abstract class AbstractRequestEnricherTestCase extends AbstractEnricherTestCase {

  @Test
  @Description("Enrich a request that contains attachments")
  public void enrich() throws Exception {
    SoapAttachment attachment = SoapTestUtils.getTestAttachment();
    String providedBody = SoapTestUtils.getRequestResource(SoapTestUtils.UPLOAD_ATTACHMENT);
    AttachmentRequestEnricher enricher = getEnricher();
    String request = enricher.enrichRequest(providedBody, asList(attachment));
    SoapTestUtils.assertSimilarXml(getExpectedResult(), request);
  }

  protected abstract AttachmentRequestEnricher getEnricher();

  protected abstract String getExpectedResult();
}
