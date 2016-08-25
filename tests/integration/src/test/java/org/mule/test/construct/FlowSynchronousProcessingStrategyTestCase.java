/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleMessage;

public class FlowSynchronousProcessingStrategyTestCase extends FlowDefaultProcessingStrategyTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow-synchronous-processing-strategy-config.xml";
  }

  @Override
  public void oneWay() throws Exception {
    flowRunner(FLOW_NAME).withPayload(TEST_PAYLOAD).asynchronously().run();
    MuleMessage message = muleContext.getClient().request("test://out", RECEIVE_TIMEOUT).getRight().get();
    assertThat(message.getOutboundProperty(PROCESSOR_THREAD), is(Thread.currentThread().getName()));
  }
}
