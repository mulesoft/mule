/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.function;

import static java.lang.Thread.currentThread;
import static java.util.Optional.of;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


public class LookupFunctionTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expected = none();

  private String flowName;
  private Flow flow;
  private ConfigurationComponentLocator locator;

  private LookupFunction lookup;
  private CompletableFuture<Event> lookupResult;

  @Before
  public void before() throws MuleException {
    flowName = "flowName";
    flow = mock(Flow.class);

    when(flow.execute(any(Event.class))).thenAnswer(inv -> lookupResult);

    locator = mock(ConfigurationComponentLocator.class);
    when(locator.find(Location.builder().globalName(flowName).build())).thenReturn(of(flow));

    lookup = new LookupFunction(locator, mock(SchedulerService.class));
    PrivilegedEvent.setCurrentEvent((PrivilegedEvent) testEvent());
  }

  @Test
  public void happyPath() throws MuleException {
    lookupResult = completedFuture(CoreEvent.builder(testEvent()).message(Message.of("Goodbye World!")).build());
    final TypedValue result = (TypedValue) lookup.call(new Object[] {flowName, "Hello World!", 1}, NULL_BINDING_CONTEXT);

    assertThat(result.getValue(), equalTo("Goodbye World!"));
  }

  @Test
  public void interruption() throws MuleException {
    lookupResult = new CompletableFuture<>();
    currentThread().interrupt();
    assertThat(lookupResult.isCancelled(), is(false));

    try {
      expected.expectCause(instanceOf(InterruptedException.class));
      lookup.call(new Object[] {flowName, "Hello World!", 1}, NULL_BINDING_CONTEXT);
    } finally {
      assertThat(lookupResult.isCancelled(), is(true));
      assertThat(currentThread().isInterrupted(), is(true));
    }
  }

  @Test
  public void timeout() throws MuleException {
    lookupResult = new CompletableFuture<>();
    assertThat(lookupResult.isCancelled(), is(false));

    try {
      expected.expectMessage("Flow 'flowName' has timed out after 1 millis");
      expected.expectCause(instanceOf(TimeoutException.class));
      lookup.call(new Object[] {flowName, "Hello World!", 1}, NULL_BINDING_CONTEXT);
    } finally {
      assertThat(lookupResult.isCancelled(), is(true));
    }
  }
}
