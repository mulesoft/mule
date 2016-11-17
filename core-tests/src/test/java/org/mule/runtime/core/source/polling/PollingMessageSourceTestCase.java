/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.source.polling;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.source.polling.MessageProcessorPollingOverride.NullOverride;
import org.mule.runtime.core.source.polling.schedule.FixedFrequencyScheduler;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.List;

import org.junit.Test;

public class PollingMessageSourceTestCase extends AbstractMuleContextTestCase {

  @Test
  public void nullResponseFromNestedMP() throws Exception {
    PollingMessageSource pollingMessageSource = createMessageSource(event -> null);

    SensingNullMessageProcessor flow = getSensingNullMessageProcessor();
    pollingMessageSource.setListener(flow);

    pollingMessageSource.poll();

    assertNull(flow.event);
  }

  @Test
  public void nullPayloadResponseFromNestedMP() throws Exception {

    PollingMessageSource pollingMessageSource =
        createMessageSource(event -> Event.builder(event).message(InternalMessage.builder().nullPayload().build()).build());

    SensingNullMessageProcessor flow = getSensingNullMessageProcessor();
    pollingMessageSource.setListener(flow);

    pollingMessageSource.poll();

    assertNull(flow.event);
  }

  @Test
  public void emptyStringResponseFromNestedMP() throws Exception {

    PollingMessageSource pollingMessageSource =
        createMessageSource(event -> Event.builder(event).message(InternalMessage.builder().payload("").build()).build());

    SensingNullMessageProcessor flow = getSensingNullMessageProcessor();
    pollingMessageSource.setListener(flow);

    pollingMessageSource.poll();

    assertNotNull(flow.event);
  }

  @Test
  public void disposeScheduler() throws Exception {
    reset(muleContext.getSchedulerService());
    PollingMessageSource pollingMessageSource = createMessageSource(event -> null);

    verify(muleContext.getSchedulerService()).ioScheduler();
    List<Scheduler> createdSchedulers =
        ((SimpleUnitTestSupportSchedulerService) (muleContext.getSchedulerService())).getCreatedSchedulers();
    pollingMessageSource.start();

    Scheduler pollScheduler = createdSchedulers.get(createdSchedulers.size() - 1);

    verify(pollScheduler).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());

    pollingMessageSource.stop();
    pollingMessageSource.dispose();

    verify(pollScheduler).stop(anyLong(), any());
  }

  private PollingMessageSource createMessageSource(Processor processor) throws Exception {
    PollingMessageSource pollingMessageSource =
        new PollingMessageSource(muleContext, processor, new NullOverride(), scheduler());
    pollingMessageSource.setFlowConstruct(getTestFlow(muleContext));
    pollingMessageSource.initialise();
    return pollingMessageSource;
  }

  private FixedFrequencyScheduler scheduler() {
    FixedFrequencyScheduler factory = new FixedFrequencyScheduler();
    factory.setFrequency(1000);
    return factory;
  }

}
