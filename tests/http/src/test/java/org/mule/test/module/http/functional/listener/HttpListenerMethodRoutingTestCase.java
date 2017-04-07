/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static org.mule.service.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.core.api.Event;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.http.functional.AbstractHttpTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import ru.yandex.qatools.allure.annotations.Features;

@RunnerDelegateTo(Parameterized.class)
@Features(HTTP_EXTENSION)
public class HttpListenerMethodRoutingTestCase extends AbstractHttpTestCase {

  @Rule
  public DynamicPort listenPort = new DynamicPort("port");

  @Rule
  public SystemProperty path = new SystemProperty("path", "path");

  private final String method;
  private final String expectedContent;

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{"GET", "GET"}, {"POST", "POST"}, {"OPTIONS", "OPTIONS-DELETE"},
        {"DELETE", "OPTIONS-DELETE"}, {"PUT", "ALL"}});
  }

  public HttpListenerMethodRoutingTestCase(String method, String expectedContent) {
    this.method = method;
    this.expectedContent = expectedContent;
  }

  @Override
  protected String getConfigFile() {
    return "http-listener-method-routing-config.xml";
  }

  @Test
  public void callWithMethod() throws Exception {
    sendRequestAndAssertMethod(TEST_MESSAGE);
    assertThat(getPayloadAsString(muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get()),
               equalTo(TEST_MESSAGE));
  }

  @Test
  public void callWithMethodEmptyBody() throws Exception {
    sendRequestAndAssertMethod("");
  }

  private void sendRequestAndAssertMethod(String payload) throws Exception {
    Event event = flowRunner("requestFlow").withPayload(payload).withVariable("method", method).run();

    HttpResponseAttributes attributes = (HttpResponseAttributes) event.getMessage().getAttributes();
    assertThat(attributes.getStatusCode(), is(OK.getStatusCode()));
    assertThat(event.getMessageAsString(muleContext), is(expectedContent));
  }

}
