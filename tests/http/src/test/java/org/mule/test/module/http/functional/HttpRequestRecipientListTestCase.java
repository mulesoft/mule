/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.List;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore
public class HttpRequestRecipientListTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");
  @Rule
  public DynamicPort port2 = new DynamicPort("port2");
  @Rule
  public DynamicPort port3 = new DynamicPort("port3");

  @Override
  protected String getConfigFile() {
    return "http-request-recipient-list-config.xml";
  }

  @Test
  public void recipientListWithHttpUrlsWithResponse() throws Exception {
    final Event response = flowRunner("recipientListFlow").withPayload(TEST_MESSAGE)
        .withInboundProperty("urls", new String[] {getUrlForPort(port1), getUrlForPort(port2), getUrlForPort(port3)}).run();

    assertThat(response, notNullValue());
    assertThat(response.getMessage().getPayload().getValue(), IsInstanceOf.instanceOf(List.class));
    Message aggregatedResponse = response.getMessage();
    assertThat(((List<InternalMessage>) aggregatedResponse.getPayload().getValue()).size(), is(3));
    final InternalMessage[] messages =
        (InternalMessage[]) ((List<InternalMessage>) aggregatedResponse.getPayload().getValue()).toArray();
    for (int i = 0; i < messages.length; i++) {
      InternalMessage message = messages[i];
      assertThat(message, notNullValue());
      assertThat(getPayloadAsString(message), is("inXFlowResponse".replace("X", String.valueOf(i + 1))));
    }
  }

  private String getUrlForPort(DynamicPort port) {
    return String.format("http://localhost:%s/path", port.getNumber());
  }
}
