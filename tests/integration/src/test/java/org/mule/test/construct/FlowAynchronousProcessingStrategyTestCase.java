/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.mule.runtime.core.processor.strategy.LegacyAsynchronousProcessingStrategyFactory.SYNCHRONOUS_EVENT_ERROR_MESSAGE;

import org.mule.runtime.core.exception.MessagingException;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class FlowAynchronousProcessingStrategyTestCase extends FlowDefaultProcessingStrategyTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow-asynchronous-processing-strategy-config.xml";
  }

  @Override
  public void requestResponse() throws Exception {
    super.requestResponse();
  }

  @Override
  public void requestResponseTransacted() throws Exception {
    setUpExpectedException();
    super.requestResponseTransacted();
  }

  @Override
  public void oneWayTransacted() throws Exception {
    setUpExpectedException();
    super.oneWayTransacted();
  }

  private void setUpExpectedException() {
    expectedException.expect(MessagingException.class);
    expectedException.expectMessage(SYNCHRONOUS_EVENT_ERROR_MESSAGE);
  }

}
