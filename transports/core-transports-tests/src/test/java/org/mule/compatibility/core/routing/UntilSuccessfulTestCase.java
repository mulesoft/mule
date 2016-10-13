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
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.store.ListableObjectStore;
import org.mule.runtime.core.util.store.SimpleMemoryObjectStore;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Prober;

import org.junit.Test;

public class UntilSuccessfulTestCase extends AbstractMuleContextTestCase {

  public static class ConfigurableMessageProcessor implements Processor {

    private volatile int eventCount;
    private volatile Event event;
    private volatile int numberOfFailuresToSimulate;

    @Override
    public Event process(final Event evt) throws MuleException {
      eventCount++;
      if (numberOfFailuresToSimulate-- > 0) {
        throw new RuntimeException("simulated problem");
      }
      this.event = evt;
      return evt;
    }

    public Event getEventReceived() {
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

  private ListableObjectStore<Event> objectStore;
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
    untilSuccessful.setMessagingExceptionHandler(muleContext.getDefaultErrorHandler());
    untilSuccessful.setFlowConstruct(getTestFlow(muleContext));
    untilSuccessful.setMaxRetries(2);

    if (millisBetweenRetries != null) {
      untilSuccessful.setMillisBetweenRetries(millisBetweenRetries);
    }

    objectStore = new SimpleMemoryObjectStore<>();
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

    Event event = eventBuilder().message(InternalMessage.of("ERROR")).build();
    assertSame(event, untilSuccessful.process(event));

    pollingProber.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        verify(dlqEndpoint).process(any(Event.class));
        return true;
      }

      @Override
      public String describeFailure() {
        return "Dead letter queue was not called";
      }
    });
  }
}
