/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm.functional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.lifecycle.Callable;

import org.junit.Before;
import org.junit.Test;

public class EndpointContentTypeTestCase extends FunctionalTestCase {

  private MuleClient client;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/config/content-type-setting-endpoint-configs-flow.xml";
  }

  @Before
  public void before() {
    client = muleContext.getClient();
  }

  @Test
  public void testXmlContentType() throws Exception {
    MuleMessage result =
        client.send("vm://in1?connector=vm-in1", MuleMessage.builder().payload("<OK/>").mediaType(MediaType.XML).build())
            .getRight();
    assertNotNull(result.getExceptionPayload());
    assertTrue(result.getExceptionPayload().getException() instanceof MessagingException);
  }

  @Test
  public void testPlainContentType() throws Exception {
    EchoComponent.setExpectedContentType("text/plain");
    MuleMessage response =
        client.send("vm://in1?connector=vm-in1", MuleMessage.builder().payload("OK").mediaType(MediaType.TEXT).build())
            .getRight();
    assertNotNull(response);
    assertEquals("OK", response.getPayload());
  }

  @Test
  public void testDefaultContentType() throws Exception {
    EchoComponent.setExpectedContentType("text/plain");
    MuleMessage response = client.send("vm://in1?connector=vm-in1", MuleMessage.builder().payload("OK").build()).getRight();
    assertNotNull(response);
    assertEquals("OK", response.getPayload());
  }

  @Test
  public void testXmlContentTypePlainPayload() throws Exception {
    EchoComponent.setExpectedContentType("text/xml");
    MuleMessage result =
        client.send("vm://in2?connector=vm-in2", MuleMessage.builder().payload("OK").mediaType(MediaType.TEXT).build())
            .getRight();
    assertNotNull(result.getExceptionPayload());
    assertTrue(result.getExceptionPayload().getException() instanceof MessagingException);
  }

  public static class EchoComponent implements Callable {

    static String expectedContentType;

    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
      MuleMessage message = eventContext.getMessage();
      final MediaType parse = MediaType.parse(expectedContentType);
      assertThat(message.getDataType().getMediaType().getPrimaryType(), is(parse.getPrimaryType()));
      assertThat(message.getDataType().getMediaType().getSubType(), is(parse.getSubType()));
      return message;
    }

    public static void setExpectedContentType(String expectedContentType) {
      EchoComponent.expectedContentType = expectedContentType;
    }
  }
}
