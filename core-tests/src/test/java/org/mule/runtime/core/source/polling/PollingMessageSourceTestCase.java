/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.source.polling;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.source.polling.MessageProcessorPollingOverride.NullOverride;
import org.mule.runtime.core.source.polling.schedule.FixedFrequencyScheduledPollFactory;
import org.mule.runtime.core.source.polling.schedule.ScheduledPoll;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Collection;

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

    PollingMessageSource pollinMessageSource = createMessageSource(event -> null);

    Collection<ScheduledPoll> allSchedulers = getAllSchedulers();
    assertThat(allSchedulers.size(), is(1));

    ScheduledPoll scheduler = allSchedulers.iterator().next();

    pollinMessageSource.stop();
    pollinMessageSource.dispose();

    assertThat(getAllSchedulers().size(), is(0));
    verify(scheduler).dispose();
  }

  private Collection<ScheduledPoll> getAllSchedulers() {
    return muleContext.getRegistry().lookupObjects(ScheduledPoll.class);
  }

  private PollingMessageSource createMessageSource(Processor processor) throws Exception {
    PollingMessageSource pollingMessageSource =
        new PollingMessageSource(muleContext, processor, new NullOverride(), schedulerFactory());
    pollingMessageSource.setFlowConstruct(getTestFlow(muleContext));
    pollingMessageSource.initialise();
    return pollingMessageSource;
  }

  private FixedFrequencyScheduledPollFactory schedulerFactory() {
    FixedFrequencyScheduledPollFactory factory = new FixedFrequencyScheduledPollFactory() {

      @Override
      public ScheduledPoll doCreate(String name, final Runnable job) {
        return spy(super.doCreate(name, job));
      }
    };
    factory.setFrequency(1000);
    factory.setMuleContext(muleContext);
    return factory;
  }

}
