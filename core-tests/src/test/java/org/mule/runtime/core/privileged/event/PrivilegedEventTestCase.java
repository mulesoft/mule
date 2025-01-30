/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.event;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.builder;

import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;

import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.event.DefaultEventBuilder;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class PrivilegedEventTestCase extends AbstractMuleTestCase {

  @Test
  public void whenNoLoggingVariableIsAddedThenTheMapIsNotCreated() throws MuleException {
    PrivilegedEvent event = builder(testEvent()).build();
    assertThat(event.getLoggingVariables(), is(empty()));
  }

  @Test
  public void whenBuilderIsUsedWhenShallowCopyInternalParametersAreRemoved() throws MuleException {
    InternalEvent internalEvent = ((DefaultEventBuilder) getEventBuilder())
        .internalParameters(singletonMap("key1", "value1"))
        .message(of(TEST_PAYLOAD)).build();

    PrivilegedEvent event = builder(internalEvent, true).build();
    assertThat(((InternalEvent) event).getInternalParameters(), is(anEmptyMap()));
  }

  @Test
  public void whenBuilderIsNotUsedWhenShallowCopyInternalParametersAreCopied() throws MuleException {
    InternalEvent internalEvent = ((DefaultEventBuilder) getEventBuilder())
        .internalParameters(singletonMap("key1", "value1"))
        .message(of(TEST_PAYLOAD)).build();

    PrivilegedEvent event = builder(internalEvent, false).build();
    assertThat(((InternalEvent) event).getInternalParameters(), hasEntry("key1", "value1"));
  }

  @Test
  public void whenAVariableIsAddedThenTheMapContainsIt() throws MuleException {
    PrivilegedEvent event = builder(testEvent()).addLoggingVariable("some", "value").build();
    assertThat(event.getLoggingVariables(), is(not(empty())));
    event.getLoggingVariables().ifPresent(vars -> assertThat(vars.get("some"), is("value")));
  }

  @Test
  public void newEventInstanceIsNotCreatedWhenALoggingVariableIsNotAddedToEmptyEvent() throws MuleException {
    CoreEvent originalEvent = testEvent();
    PrivilegedEvent newEvent = builder(originalEvent).build();
    assertThat(newEvent, is(originalEvent));
  }

  @Test
  public void newEventInstanceIsNotCreatedWhenALoggingVariableIsNotAddedToEventWithPreviousVariables() throws MuleException {
    CoreEvent originalEvent = builder(testEvent()).addLoggingVariable("some", "value").build();
    PrivilegedEvent newEvent = builder(originalEvent).build();
    assertThat(newEvent, is(originalEvent));
  }

  @Test
  public void newEventInstanceIsCreatedWhenALoggingVariableIsAddedToEmptyEvent() throws MuleException {
    CoreEvent originalEvent = testEvent();
    PrivilegedEvent newEvent = builder(originalEvent).addLoggingVariable("some", "value").build();
    assertThat(newEvent, is(not(originalEvent)));
  }

  @Test
  public void newEventInstanceIsCreatedWhenALoggingVariableIsAddedToEventWithPreviousVariables() throws MuleException {
    CoreEvent originalEvent = builder(testEvent()).addLoggingVariable("some", "value").build();
    PrivilegedEvent newEvent = builder(originalEvent).addLoggingVariable("new", "value").build();
    assertThat(newEvent, is(not(originalEvent)));
  }

  @Test
  public void removeLoggingVariableFromEmptyEventDoesNotCreateNewEvent() throws MuleException {
    CoreEvent originalEvent = testEvent();
    PrivilegedEvent newEvent = builder(originalEvent).removeLoggingVariable("unexistent").build();
    assertThat(newEvent, is(originalEvent));
  }

  @Test
  public void removeLoggingVariableOnEventWithoutThatVariableDoesNotCreateNewEvent() throws MuleException {
    CoreEvent originalEvent = builder(testEvent()).addLoggingVariable("some", "value").build();
    PrivilegedEvent newEvent = builder(originalEvent).removeLoggingVariable("unexistent").build();
    assertThat(newEvent, is(originalEvent));
  }

  @Test
  public void removeLoggingVariableOnEventWithThatVariableCreatesNewEvent() throws MuleException {
    CoreEvent originalEvent = builder(testEvent()).addLoggingVariable("some", "value").build();
    PrivilegedEvent newEvent = builder(originalEvent).removeLoggingVariable("some").build();
    assertThat(newEvent, is(not(originalEvent)));
  }

  @Test
  public void removeLoggingVariable() throws MuleException {
    CoreEvent originalEvent = builder(testEvent()).addLoggingVariable("some", "value").build();
    PrivilegedEvent newEvent = builder(originalEvent).removeLoggingVariable("some").build();
    assertThat(newEvent.getLoggingVariables(), is(not(empty())));
    newEvent.getLoggingVariables().ifPresent(vars -> assertThat(vars.keySet(), not(contains("some"))));
  }
}
