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

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.schedule.Scheduler;
import org.mule.runtime.core.source.polling.MessageProcessorPollingOverride.NullOverride;
import org.mule.runtime.core.source.polling.schedule.FixedFrequencySchedulerFactory;
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
        createMessageSource(event -> MuleEvent.builder(event).message(MuleMessage.builder().nullPayload().build()).build());

    SensingNullMessageProcessor flow = getSensingNullMessageProcessor();
    pollingMessageSource.setListener(flow);

    pollingMessageSource.poll();

    assertNull(flow.event);
  }

  @Test
  public void emptyStringResponseFromNestedMP() throws Exception {

    PollingMessageSource pollingMessageSource =
        createMessageSource(event -> MuleEvent.builder(event).message(MuleMessage.builder().payload("").build()).build());

    SensingNullMessageProcessor flow = getSensingNullMessageProcessor();
    pollingMessageSource.setListener(flow);

    pollingMessageSource.poll();

    assertNotNull(flow.event);
  }

  @Test
  public void disposeScheduler() throws Exception {

    PollingMessageSource pollinMessageSource = createMessageSource(event -> null);

    Collection<Scheduler> allSchedulers = getAllSchedulers();
    assertThat(allSchedulers.size(), is(1));

    Scheduler scheduler = allSchedulers.iterator().next();

    pollinMessageSource.stop();
    pollinMessageSource.dispose();

    assertThat(getAllSchedulers().size(), is(0));
    verify(scheduler).dispose();
  }

  private Collection<Scheduler> getAllSchedulers() {
    return muleContext.getRegistry().lookupObjects(Scheduler.class);
  }

  private PollingMessageSource createMessageSource(MessageProcessor processor) throws Exception {
    PollingMessageSource pollingMessageSource =
        new PollingMessageSource(muleContext, processor, new NullOverride(), schedulerFactory());
    pollingMessageSource.setFlowConstruct(getTestFlow());
    pollingMessageSource.initialise();
    return pollingMessageSource;
  }

  private FixedFrequencySchedulerFactory schedulerFactory() {
    FixedFrequencySchedulerFactory factory = new FixedFrequencySchedulerFactory() {

      @Override
      public Scheduler doCreate(String name, final Runnable job) {
        return spy(super.doCreate(name, job));
      }
    };
    factory.setFrequency(1000);
    factory.setMuleContext(muleContext);
    return factory;
  }

}
