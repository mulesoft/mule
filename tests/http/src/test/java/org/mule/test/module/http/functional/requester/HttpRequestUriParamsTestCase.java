/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.exception.MessagingException;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 *
 */
public class HttpRequestUriParamsTestCase extends AbstractHttpRequestTestCase {

  @Override
  protected String getConfigFile() {
    return "http-request-uri-params-config.xml";
  }

  @Test
  public void sendsUriParamsFromList() throws Exception {
    flowRunner("uriParamList").withPayload(TEST_MESSAGE).withFlowVariable("paramName", "testParam2")
        .withFlowVariable("paramValue", "testValue2").run();
    assertThat(uri, equalTo("/testPath/testValue1/testValue2"));
  }

  @Test
  public void sendsUriParamsFromMap() throws Exception {
    Map<String, String> params = new HashMap<>();
    params.put("testParam1", "testValue1");
    params.put("testParam2", "testValue2");
    flowRunner("uriParamMap").withPayload(TEST_MESSAGE).withFlowVariable("params", params).run();

    assertThat(uri, equalTo("/testPath/testValue1/testValue2"));
  }

  @Test
  public void overridesUriParams() throws Exception {
    Map<String, String> params = new HashMap<>();
    params.put("testParam1", "testValueNew");
    params.put("testParam2", "testValue2");
    flowRunner("uriParamOverride").withPayload(TEST_MESSAGE).withFlowVariable("params", params).run();

    assertThat(uri, equalTo("/testPath/testValueNew/testValue2"));
  }

  @Test
  public void sendsUriParamsIfNull() throws Exception {
    MessagingException expectedException = flowRunner("uriParamNull").runExpectingException();
    assertThat(expectedException.getCause(), instanceOf(NullPointerException.class));
    assertThat(expectedException.getMessage(), containsString("Expression {testParam2} evaluated to null."));
  }

}
