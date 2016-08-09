/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.ws.functional;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.util.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

@Ignore("See MULE-9203")
public class MtomFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase {

  private static final String TEST_FILE_ATTACHMENT = "TestAttachments.wsdl";

  @Override
  protected String getConfigFile() {
    return "mtom-config.xml";
  }

  @Test
  public void uploadAttachmentTest() throws Exception {
    String request = String.format("<ns:uploadAttachment xmlns:ns=\"http://consumer.ws.module.runtime.mule.org/\">"
        + "<fileName>%s</fileName><attachment>"
        + "<xop:Include xmlns:xop=\"http://www.w3.org/2004/08/xop/include\" href=\"cid:testAttachmentId\"/>"
        + "</attachment></ns:uploadAttachment>", TEST_FILE_ATTACHMENT);

    MuleEvent event = flowRunner("clientUploadAttachment").withPayload(request)
        .withOutboundAttachment("testAttachmentId", buildDataHandler(TEST_FILE_ATTACHMENT)).run();

    String expected = "<ns2:uploadAttachmentResponse xmlns:ns2=\"http://consumer.ws.module.runtime.mule.org/\">"
        + "<result>OK</result></ns2:uploadAttachmentResponse>";

    assertXMLEqual(expected, getPayloadAsString(event.getMessage()));
  }

  @Test
  public void downloadAttachmentTest() throws Exception {
    String request = String.format("<ns:downloadAttachment xmlns:ns=\"http://consumer.ws.module.runtime.mule.org/\">"
        + "<fileName>%s</fileName></ns:downloadAttachment>", TEST_FILE_ATTACHMENT);

    MuleEvent event = flowRunner("clientDownloadAttachment").withPayload(request).run();
    assertAttachmentInResponse(event.getMessage(), TEST_FILE_ATTACHMENT);
  }

  @Test
  public void echoAttachment() throws Exception {
    String request = "<ns:echoAttachment xmlns:ns=\"http://consumer.ws.module.runtime.mule.org/\"><attachment>"
        + "<xop:Include xmlns:xop=\"http://www.w3.org/2004/08/xop/include\" href=\"cid:testAttachmentId\"/>"
        + "</attachment></ns:echoAttachment>";

    MuleEvent event = flowRunner("clientEchoAttachment").withPayload(request)
        .withOutboundAttachment("testAttachmentId", buildDataHandler(TEST_FILE_ATTACHMENT)).run();

    assertAttachmentInResponse(event.getMessage(), TEST_FILE_ATTACHMENT);
  }

  protected DataHandler buildDataHandler(String fileName) {
    File file = new File(IOUtils.getResourceAsUrl(fileName, getClass()).getPath());
    return new DataHandler(new FileDataSource(file));
  }

  private void assertAttachmentInResponse(MuleMessage message, String fileName) throws Exception {
    final MultiPartPayload multiPartPayload = (MultiPartPayload) message.getPayload();
    assertThat(multiPartPayload.getParts(), hasSize(1));

    String attachmentId = extractAttachmentId(getPayloadAsString(message));

    final org.mule.runtime.api.message.MuleMessage part = multiPartPayload.getPart(attachmentId);

    assertNotNull(part.getPayload());

    InputStream expected = IOUtils.getResourceAsStream(fileName, getClass());

    assertTrue(IOUtils.contentEquals(expected, part.getPayload()));
  }


  private String extractAttachmentId(String payload) {
    Pattern pattern = Pattern.compile("href=\"cid:(.*?)\"");
    Matcher matcher = pattern.matcher(payload);

    if (matcher.find()) {
      return matcher.group(1);
    }

    throw new IllegalArgumentException("Payload does not contain an attachment id");
  }


}
