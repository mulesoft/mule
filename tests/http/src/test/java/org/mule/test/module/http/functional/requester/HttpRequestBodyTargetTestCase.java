/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.Event;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;

@Features(HTTP_EXTENSION)
public class HttpRequestBodyTargetTestCase extends AbstractHttpRequestTestCase {

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
    flowRunner("customSourceFlow").withPayload(payload).withVariable("customSource", "customValue").run();
    assertThat(body, equalTo("customValue"));
  }

  @Test
  public void responseBodyToPayloadTarget() throws Exception {
    Event event = flowRunner("payloadTargetFlow").withPayload(TEST_MESSAGE).run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo(DEFAULT_RESPONSE));
  }
}
