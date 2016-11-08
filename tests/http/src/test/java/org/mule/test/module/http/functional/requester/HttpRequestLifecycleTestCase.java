/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.construct.AbstractFlowConstruct;
import org.mule.runtime.core.retry.RetryPolicyExhaustedException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HttpRequestLifecycleTestCase extends AbstractHttpRequestTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "http-request-lifecycle-config.xml";
  }

  @Test
  public void stoppedConfigMakesRequesterFail() throws Exception {
    verifyRequest();
    Stoppable requestConfig = muleContext.getRegistry().lookupObject("requestConfig");
    requestConfig.stop();
    try {
      expectedException.expectCause(isA(RetryPolicyExhaustedException.class));
      runFlow("simpleRequest");
    } finally {
      ((Startable) requestConfig).start();
    }
  }

  @Test
  public void stoppedConfigDoesNotAffectAnother() throws Exception {
    verifyRequest();
    Stoppable requestConfig = muleContext.getRegistry().lookupObject("requestConfig");
    requestConfig.stop();
    verifyRequest("otherRequest");
    ((Startable) requestConfig).start();
  }

  @Test
  public void restartConfig() throws Exception {
    verifyRequest();
    Object requestConfig = muleContext.getRegistry().lookupObject("requestConfig");
    ((Stoppable) requestConfig).stop();
    ((Startable) requestConfig).start();
    verifyRequest();
  }

  @Test
  public void restartFlow() throws Exception {
    verifyRequest();
    AbstractFlowConstruct flow = (AbstractFlowConstruct) muleContext.getRegistry().lookupFlowConstruct("simpleRequest");
    flow.stop();
    flow.start();
    verifyRequest();
  }

  private void verifyRequest() throws Exception {
    verifyRequest("simpleRequest");
  }

  private void verifyRequest(String flowName) throws Exception {
    assertThat(runFlow(flowName).getMessage(), hasPayload(equalTo((DEFAULT_RESPONSE))));
  }
}
