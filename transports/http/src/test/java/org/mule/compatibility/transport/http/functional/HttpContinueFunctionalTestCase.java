/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.mule.compatibility.transport.http.HttpConnector;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.lang.time.StopWatch;
import org.junit.Rule;
import org.junit.Test;

public class HttpContinueFunctionalTestCase extends FunctionalTestCase {

  /**
   * HttpClient has default 3 seconds wait for Expect-Continue calls.
   */
  private static final int DEFAULT_HTTP_CLIENT_CONTINUE_WAIT = 3000;

  protected StopWatch stopWatch;

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "http-functional-test-flow.xml";
  }

  @Test
  public void testSendWithContinue() throws Exception {
    stopWatch = new StopWatch();
    MuleClient client = muleContext.getClient();

    // Need to use Http1.1 for Expect: Continue
    HttpClientParams params = new HttpClientParams();
    params.setVersion(HttpVersion.HTTP_1_1);
    params.setBooleanParameter(HttpClientParams.USE_EXPECT_CONTINUE, true);

    Map<String, Serializable> props = new HashMap<>();
    props.put(HttpConnector.HTTP_PARAMS_PROPERTY, params);

    stopWatch.start();
    MuleMessage result = client.send("clientEndpoint", TEST_MESSAGE, props).getRight();
    stopWatch.stop();

    assertNotNull(result);
    assertEquals(TEST_MESSAGE + " Received", getPayloadAsString(result));

    if (stopWatch.getTime() > DEFAULT_HTTP_CLIENT_CONTINUE_WAIT) {
      fail("Server did not handle Expect=100-continue header properly,");
    }
  }
}
