/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.generator;

import org.mule.extension.ws.internal.generator.attachment.AttachmentRequestEnricher;
import org.mule.extension.ws.internal.generator.attachment.MtomRequestEnricher;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Web Service Consumer")
@Stories({"Attachments", "MTOM", "Request Generation"})
public class MtomRequestEnricherTestCase extends AbstractRequestEnricherTestCase {

  @Override
  @Step("Returns an MTOM enricher that adds an XOP element to the XML referencing the attachment in the multipart message")
  protected AttachmentRequestEnricher getEnricher() {
    return new MtomRequestEnricher();
  }

  @Override
  protected String getExpectedResult() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<con:uploadAttachment xmlns:con=\"http://service.ws.extension.mule.org/\">"
        + "<attachment>"
        + "<xop:Include xmlns:xop=\"http://www.w3.org/2004/08/xop/include\" href=\"cid:attachment-id\"/>"
        + "</attachment>"
        + "</con:uploadAttachment>";
  }
}
