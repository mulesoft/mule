/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static org.hamcrest.Matchers.containsString;

import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.module.http.functional.AbstractHttpTestCase;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.yandex.qatools.allure.annotations.Features;

//TODO: MULE-10281 - Review error handling in requester
@Ignore
@Features(HTTP_EXTENSION)
public class HttpRequestFailureTestCase extends AbstractHttpTestCase {

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");
  @Rule
  public DynamicPort httpsPort = new DynamicPort("httpsPort");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "http-request-failure-config.xml";
  }

  @Test
  public void simpleRequest() throws Exception {
    verifyErrorMessageUri(String.format("http://localhost:%s/testPath", httpPort.getValue()), "simple");
  }

  @Test
  public void httpsRequest() throws Exception {
    verifyErrorMessageUri(String.format("https://localhost:%s/test/testPath", httpsPort.getValue()), "secure");
  }

  @Test
  public void basicAuthRequest() throws Exception {
    verifyErrorMessageUri(String.format("http://localhost:%s/testPath", httpPort.getValue()), "basicAuth");
  }

  private void verifyErrorMessageUri(String uri, String flowName) throws Exception {
    expectedException.expectMessage(containsString(uri));
    runFlow(flowName);
  }
}
