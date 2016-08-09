/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.util.IOUtils;

import java.io.InputStream;

import org.junit.Test;


public class HttpRequestSourceTargetTestCase extends AbstractHttpRequestTestCase {

  @Override
  protected String getConfigFile() {
    return "http-request-source-target-config.xml";
  }

  @Test
  public void requestBodyFromPayloadSource() throws Exception {
    flowRunner("payloadSourceFlow").withPayload(TEST_MESSAGE).run();
    assertThat(body, equalTo(TEST_MESSAGE));
  }

  @Test
  public void requestBodyFromCustomSource() throws Exception {
    sendRequestFromCustomSourceAndAssertResponse(TEST_MESSAGE);
  }

  @Test
  public void requestBodyFromCustomSourceAndNullPayload() throws Exception {
    sendRequestFromCustomSourceAndAssertResponse(null);
  }

  private void sendRequestFromCustomSourceAndAssertResponse(Object payload) throws Exception {
    flowRunner("customSourceFlow").withPayload(payload).withFlowVariable("customSource", "customValue").run();
    assertThat(body, equalTo("customValue"));
  }

  @Test
  public void responseBodyToPayloadTarget() throws Exception {
    MuleEvent event = flowRunner("payloadTargetFlow").withPayload(TEST_MESSAGE).run();
    assertThat(getPayloadAsString(event.getMessage()), equalTo(DEFAULT_RESPONSE));
  }

  @Test
  public void responseBodyToCustomTarget() throws Exception {
    MuleEvent event = flowRunner("customTargetFlow").withPayload(TEST_MESSAGE).run();
    MuleMessage customTarget = event.getFlowVariable("customTarget");
    assertThat(customTarget, notNullValue());
    assertThat(IOUtils.toString((InputStream) customTarget.getPayload()), equalTo(DEFAULT_RESPONSE));
    assertThat(getPayloadAsString(event.getMessage()), equalTo(TEST_MESSAGE));
  }

}
