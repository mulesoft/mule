/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.event;

import static java.util.Optional.empty;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.builder;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
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
