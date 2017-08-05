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
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.NestedProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NestedProcessorValueResolverTestCase extends AbstractMuleContextTestCase {

  @Mock
  private Processor messageProcessor;

  @Before
  public void before() throws Exception {
    final Event testEvent = testEvent();
    when(messageProcessor.process(any(Event.class))).thenReturn(testEvent);
  }

  @Test
  public void yieldsNestedProcessor() throws Exception {
    NestedProcessorValueResolver resolver = new NestedProcessorValueResolver(messageProcessor);
    resolver.setMuleContext(muleContext);
    NestedProcessor nestedProcessor = resolver.resolve(ValueResolvingContext.from(testEvent()));
    Object response = nestedProcessor.process();
    assertThat(response, is(TEST_PAYLOAD));

    ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
    verify(messageProcessor).process(captor.capture());

    Event capturedEvent = captor.getValue();
    assertThat(capturedEvent, is(testEvent()));
  }

  @Test
  public void alwaysGivesDifferentInstances() throws Exception {
    NestedProcessorValueResolver resolver = new NestedProcessorValueResolver(messageProcessor);
    resolver.setMuleContext(muleContext);
    NestedProcessor resolved1 = resolver.resolve(ValueResolvingContext.from(testEvent()));
    NestedProcessor resolved2 = resolver.resolve(ValueResolvingContext.from(testEvent()));

    assertThat(resolved1, is(not(sameInstance(resolved2))));
  }

}
