/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.transport.http.functional;

import static java.nio.charset.StandardCharsets.UTF_16;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;
import org.mule.tck.junit4.rule.DynamicPort;

import java.nio.charset.Charset;

import org.junit.Rule;
import org.junit.Test;

public class HttpOutboundDataTypeTestCase extends CompatibilityFunctionalTestCase {

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Override
  protected String getConfigFile() {
    return "http-datatype-config.xml";
  }

  @Test
  public void propagatesDataType() throws Exception {
    MuleClient client = muleContext.getClient();

    InternalMessage muleMessage = InternalMessage.builder().payload(TEST_MESSAGE)
        .mediaType(MediaType.parse(MediaType.TEXT + "; charset=" + UTF_16.name())).build();

    client.dispatch("vm://testInput", muleMessage);

    InternalMessage response = client.request("vm://testOutput", 120000).getRight().get();

    assertThat(response.getPayload().getDataType().getMediaType().getPrimaryType(), equalTo(MediaType.TEXT.getPrimaryType()));
    assertThat(response.getPayload().getDataType().getMediaType().getSubType(), equalTo(MediaType.TEXT.getSubType()));
    assertThat(response.getPayload().getDataType().getMediaType().getCharset().get(), equalTo(UTF_16));
  }

  public static class SetMediaTypeTransformer extends AbstractMessageTransformer {

    @Override
    public Object transformMessage(Event event, Charset outputEncoding) throws TransformerException {
      return InternalMessage.builder(event.getMessage()).build();
    }
  }
}
