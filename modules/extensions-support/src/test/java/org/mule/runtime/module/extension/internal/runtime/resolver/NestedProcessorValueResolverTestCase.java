/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.NestedProcessor;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NestedProcessorValueResolverTestCase extends AbstractMuleContextTestCase {

  private static final String RESPONSE = "Hello world!";

  @Mock
  private Processor messageProcessor;

  @Before
  public void before() throws Exception {
    FlowConstruct flowConstruct = MuleTestUtils.getTestFlow(muleContext);
    when(messageProcessor.process(any(Event.class)))
        .thenReturn(Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
            .message(InternalMessage.of(RESPONSE))
            .build());
  }

  @Test
  public void yieldsNestedProcessor() throws Exception {
    FlowConstruct flowConstruct = MuleTestUtils.getTestFlow(muleContext);
    Event muleEvent = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(InternalMessage.of(""))
        .build();
    NestedProcessorValueResolver resolver = new NestedProcessorValueResolver(messageProcessor);
    resolver.setMuleContext(muleContext);
    NestedProcessor nestedProcessor = resolver.resolve(muleEvent);
    Object response = nestedProcessor.process();
    assertThat((String) response, is(sameInstance(RESPONSE)));

    ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
    verify(messageProcessor).process(captor.capture());

    Event capturedEvent = captor.getValue();
    assertThat(capturedEvent, is(muleEvent));
  }

  @Test
  public void alwaysGivesDifferentInstances() throws Exception {
    FlowConstruct flowConstruct = MuleTestUtils.getTestFlow(muleContext);
    Event muleEvent = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(InternalMessage.of(""))
        .build();
    NestedProcessorValueResolver resolver = new NestedProcessorValueResolver(messageProcessor);
    resolver.setMuleContext(muleContext);
    NestedProcessor resolved1 = resolver.resolve(muleEvent);
    NestedProcessor resolved2 = resolver.resolve(muleEvent);

    assertThat(resolved1, is(not(sameInstance(resolved2))));
  }

}
