/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DefaultEventContextTestCase extends AbstractMuleContextTestCase {

  private static int BLOCK_TIMEOUT = 50;
  private static String TIMEOUT_ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE = "Timeout on blocking read";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void successWithResult() throws Exception {
    EventContext eventContext = create(getTestFlow(muleContext), "");

    Event event = testEvent();
    eventContext.success(event);

    assertThat(from(eventContext).block(), equalTo(event));
  }

  @Test
  public void successNoResult() throws Exception {
    EventContext eventContext = create(getTestFlow(muleContext), "");

    eventContext.success();

    assertThat(from(eventContext).block(), is(nullValue()));
  }

  @Test
  public void error() throws Exception {
    EventContext eventContext = create(getTestFlow(muleContext), "");

    RuntimeException exception = new RuntimeException();
    eventContext.error(exception);

    expectedException.expect(is(exception));
    from(eventContext).block();
  }

  @Test
  public void childSuccessWithResult() throws Exception {
    EventContext eventContext = create(getTestFlow(muleContext), "");
    EventContext childEventContext = DefaultEventContext.child(eventContext);

    Event event = testEvent();
    childEventContext.success(event);

    assertThat(from(childEventContext).block(), equalTo(event));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(equalTo(TIMEOUT_ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE));
    from(eventContext).blockMillis(BLOCK_TIMEOUT);
  }

  @Test
  public void childSuccessNoResult() throws Exception {
    EventContext eventContext = create(getTestFlow(muleContext), "");
    EventContext childEventContext = DefaultEventContext.child(eventContext);

    childEventContext.success();

    assertThat(from(childEventContext).block(), is(nullValue()));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(equalTo(TIMEOUT_ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE));
    from(eventContext).blockMillis(BLOCK_TIMEOUT);
  }

  @Test
  public void childError() throws Exception {
    EventContext eventContext = create(getTestFlow(muleContext), "");
    EventContext childEventContext = DefaultEventContext.child(eventContext);

    RuntimeException exception = new RuntimeException();
    childEventContext.error(exception);

    expectedException.expect(is(exception));
    from(childEventContext).blockMillis(BLOCK_TIMEOUT);
  }

  @Test
  public void childErrorDoesNotCompleteParentContext() throws Exception {
    EventContext eventContext = create(getTestFlow(muleContext), "");
    EventContext childEventContext = DefaultEventContext.child(eventContext);

    MessagingException exception = new MessagingException(testEvent(), new RuntimeException());
    childEventContext.error(exception);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(equalTo(TIMEOUT_ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE));
    from(eventContext).blockMillis(BLOCK_TIMEOUT);
  }

}
