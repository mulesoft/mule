/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm.functional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

public class TransformerContentTypeTestCase extends CompatibilityFunctionalTestCase {

  private MuleClient client;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/config/content-type-setting-transform-configs-flow.xml";
  }

  @Before
  public void before() {
    client = muleContext.getClient();
  }

  @Test
  public void testContentTypesPlainXmlXml() throws Exception {
    EchoComponent.setExpectedMimeType("text/xml");
    InternalMessage response =
        client.send("vm://in1?connector=vm-in1", InternalMessage.builder().payload("OK").mediaType(MediaType.TEXT).build())
            .getRight();
    assertNotNull(response);
    assertEquals("OK", response.getPayload().getValue());
  }

  @Test
  public void testContentTypesAnyXmlXml() throws Exception {
    EchoComponent.setExpectedMimeType("text/xml");
    InternalMessage response =
        client.send("vm://in1?connector=vm-in1", InternalMessage.builder().payload("OK").build()).getRight();
    assertNotNull(response);
    assertEquals("OK", response.getPayload().getValue());
  }

  @Test
  public void testContentTypesXmlPlainPlain() throws Exception {
    EchoComponent.setExpectedMimeType("text/plain");
    InternalMessage response =
        client.send("vm://in2?connector=vm-in2", InternalMessage.builder().payload("OK").mediaType(MediaType.XML).build())
            .getRight();
    assertNotNull(response);
    assertEquals("OK", response.getPayload().getValue());
  }

  @Test
  public void testContentTypesAnyPlainPlain() throws Exception {
    EchoComponent.setExpectedMimeType("text/plain");
    InternalMessage response =
        client.send("vm://in2?connector=vm-in2", InternalMessage.builder().payload("OK").build()).getRight();
    assertNotNull(response);
    assertEquals("OK", response.getPayload().getValue());
  }

  public static class EchoComponent implements Callable {

    static String expectedMimeType;

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
      Message message = eventContext.getMessage();
      String contentType = message.getPayload().getDataType().getMediaType().withoutParameters().toRfcString();
      assertThat(contentType, is(expectedMimeType));
      return message;
    }

    public static void setExpectedMimeType(String expectedContentType) {
      EchoComponent.expectedMimeType = expectedContentType;
    }
  }

  public static class SetTextMediaTypeTransformer extends AbstractMessageTransformer {

    @Override
    public Object transformMessage(Event event, Charset outputEncoding) throws TransformerException {
      return InternalMessage.builder(event.getMessage()).mediaType(MediaType.TEXT.withCharset(UTF_8)).build();
    }

  }

  public static class SetXmlMediaTypeTransformer extends AbstractMessageTransformer {

    @Override
    public Object transformMessage(Event event, Charset outputEncoding) throws TransformerException {
      return InternalMessage.builder(event.getMessage()).mediaType(MediaType.XML.withCharset(UTF_8)).build();
    }

  }
}
