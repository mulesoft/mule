/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.routing;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.routing.EndpointDlqUntilSuccessful;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.store.ListableObjectStore;
import org.mule.runtime.core.util.store.SimpleMemoryObjectStore;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Prober;

import org.junit.Test;

public class UntilSuccessfulTestCase extends AbstractMuleContextTestCase {

  public static class ConfigurableMessageProcessor implements MessageProcessor {

    private volatile int eventCount;
    private volatile MuleEvent event;
    private volatile int numberOfFailuresToSimulate;

    @Override
    public MuleEvent process(final MuleEvent evt) throws MuleException {
      eventCount++;
      if (numberOfFailuresToSimulate-- > 0) {
        throw new RuntimeException("simulated problem");
      }
      this.event = evt;
      return evt;
    }

    public MuleEvent getEventReceived() {
      return event;
    }

    public int getEventCount() {
      return eventCount;
    }

    public void setNumberOfFailuresToSimulate(int numberOfFailuresToSimulate) {
      this.numberOfFailuresToSimulate = numberOfFailuresToSimulate;
    }
  }

  private EndpointDlqUntilSuccessful untilSuccessful;

  private ListableObjectStore<MuleEvent> objectStore;
  private ConfigurableMessageProcessor targetMessageProcessor;
  private Prober pollingProber = new PollingProber(10000, 500l);

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    untilSuccessful = buildUntiSuccessful(1000L);
  }

  private EndpointDlqUntilSuccessful buildUntiSuccessful(Long millisBetweenRetries) throws Exception {
    EndpointDlqUntilSuccessful untilSuccessful = new EndpointDlqUntilSuccessful();
    untilSuccessful.setMuleContext(muleContext);
    untilSuccessful.setMessagingExceptionHandler(muleContext.getDefaultExceptionStrategy());
    untilSuccessful.setFlowConstruct(getTestFlow());
    untilSuccessful.setMaxRetries(2);

    if (millisBetweenRetries != null) {
      untilSuccessful.setMillisBetweenRetries(millisBetweenRetries);
    }

    objectStore = new SimpleMemoryObjectStore<MuleEvent>();
    untilSuccessful.setObjectStore(objectStore);

    targetMessageProcessor = new ConfigurableMessageProcessor();
    untilSuccessful.addRoute(targetMessageProcessor);

    return untilSuccessful;
  }

  @Override
  protected void doTearDown() throws Exception {
    untilSuccessful.stop();
  }

  @Test
  public void testPermanentDeliveryFailureDLQ() throws Exception {
    targetMessageProcessor.setNumberOfFailuresToSimulate(Integer.MAX_VALUE);
    EndpointBuilder dlqEndpointBuilder = mock(EndpointBuilder.class);
    final OutboundEndpoint dlqEndpoint = mock(OutboundEndpoint.class);
    when(dlqEndpointBuilder.buildOutboundEndpoint()).thenReturn(dlqEndpoint);
    untilSuccessful.setDeadLetterQueue(dlqEndpointBuilder);
    untilSuccessful.initialise();
    untilSuccessful.start();

    final MuleEvent testEvent = getTestEvent("ERROR");
    assertSame(VoidMuleEvent.getInstance(), untilSuccessful.process(testEvent));

    pollingProber.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        verify(dlqEndpoint).process(any(MuleEvent.class));
        return true;
      }

      @Override
      public String describeFailure() {
        return "Dead letter queue was not called";
      }
    });
  }
}
