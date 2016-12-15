/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.generator;

import org.mule.extension.ws.internal.generator.attachment.AttachmentRequestEnricher;
import org.mule.extension.ws.internal.generator.attachment.SoapAttachmentRequestEnricher;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Web Service Consumer")
@Stories({"Attachments", "Request Generation"})
public class SoapAttachmentsRequestEnricherTestCase extends AbstractRequestEnricherTestCase {

  @Override
  @Step("Returns an attachment enricher that adds the content of the attachment encoded to base64")
  protected AttachmentRequestEnricher getEnricher() {
    return new SoapAttachmentRequestEnricher();
  }

  @Override
  protected String getExpectedResult() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<con:uploadAttachment xmlns:con=\"http://service.ws.extension.mule.org/\">"
        + "<attachment>U29tZSBDb250ZW50</attachment>"
        + "</con:uploadAttachment>";
  }
}
