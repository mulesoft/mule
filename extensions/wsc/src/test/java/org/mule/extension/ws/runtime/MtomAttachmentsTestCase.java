/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.runtime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mule.extension.ws.WscTestUtils.SIMPLE_ATTACHMENT;
import static org.mule.extension.ws.WscTestUtils.resourceAsString;
import org.mule.runtime.api.message.Message;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Web Service Consumer")
@Stories({"Attachments", "MTOM"})
public class MtomAttachmentsTestCase extends AttachmentsTestCase {

  public MtomAttachmentsTestCase() {
    super(true);
  }

  @Override
  protected void assertDownloadedAttachment(Message attachmentPart) throws XMLStreamException, IOException {
    String expectedAttachmentContent = resourceAsString(SIMPLE_ATTACHMENT);
    assertThat(IOUtils.toString((InputStream) attachmentPart.getPayload().getValue()), is(expectedAttachmentContent));
  }
}
