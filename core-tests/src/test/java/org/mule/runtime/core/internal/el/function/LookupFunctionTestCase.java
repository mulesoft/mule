/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.function;

import static java.util.Optional.of;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.core.internal.el.function.LookupFunction.isInLookupFunctionContext;

import org.mule.runtime.api.component.execution.ExecutableComponent;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

public class LookupFunctionTestCase extends AbstractMuleTestCase {

  private LookupFunction lookup;
  private ExecutableComponent flow;

  @Before
  public void before() {
    ConfigurationComponentLocator locator = mock(ConfigurationComponentLocator.class);
    flow = mock(ExecutableComponent.class, withSettings().extraInterfaces(Flow.class));
    when(locator.find(any(Location.class))).thenReturn(of(flow));
    lookup = new LookupFunction(locator);
  }

  @Test
  public void contextStateSet() throws MuleException {
    when(flow.execute(any(Event.class))).thenAnswer(invocation -> {
      assertThat(isInLookupFunctionContext(), is(true));

      CompletableFuture<PrivilegedEvent> result = new CompletableFuture<>();
      result.complete((PrivilegedEvent) testEvent());
      return result;
    });

    assertThat(isInLookupFunctionContext(), is(false));
    PrivilegedEvent.setCurrentEvent((PrivilegedEvent) testEvent());
    lookup.call(new Object[] {"flowName", TEST_PAYLOAD}, NULL_BINDING_CONTEXT);
    assertThat(isInLookupFunctionContext(), is(false));
  }
}
