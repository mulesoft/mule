/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transport;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.mule.compatibility.core.api.transport.Connector;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;
import org.mule.tck.testmodels.mule.TestMessageReceiver;

import java.util.concurrent.TimeUnit;

import javax.resource.spi.work.Work;

import org.junit.Test;
import org.mockito.Answers;

public class MessageReceiverWorkManagerTestCase extends AbstractMuleContextEndpointTestCase {

  @Test
  public void workManagerRecreatedCorrectlyAfterRestart() throws Exception {
    final Latch workExecutedLatch = new Latch();
    AbstractMessageReceiver receiver = createMessageReceiver();
    Connector receiverConnector = receiver.getConnector();
    receiverConnector.stop();
    receiverConnector.start();
    receiver.getWorkManager().scheduleWork(new Work() {

      @Override
      public void release() {}

      @Override
      public void run() {
        workExecutedLatch.release();
      }
    });
    if (!workExecutedLatch.await(1000, TimeUnit.MILLISECONDS)) {
      fail("Work should be executed and it was not");
    }

    receiverConnector.stop();
    receiverConnector.dispose();
  }

  private AbstractMessageReceiver createMessageReceiver() throws Exception {
    return new TestMessageReceiver(getTestConnector(), mock(FlowConstruct.class, Answers.RETURNS_DEEP_STUBS.get()),
                                   getTestInboundEndpoint(MessageExchangePattern.ONE_WAY));
  }

}
