/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.transport.file.FileMessageDispatcher;
import org.mule.runtime.core.api.MuleEvent;

public class SleepyFileMessageDispatcher extends FileMessageDispatcher {

  public SleepyFileMessageDispatcher(OutboundEndpoint endpoint) {
    super(endpoint);
    setMuleContext(endpoint.getMuleContext());
  }

  @Override
  protected void doDispatch(MuleEvent event) throws Exception {
    // TODO
    String sleepTime = event.getMessage().getOutboundProperty(FlowSyncAsyncProcessingStrategyTestCase.SLEEP_TIME);

    Thread.sleep(Integer.valueOf(sleepTime));

    super.doDispatch(event);

  }

}
