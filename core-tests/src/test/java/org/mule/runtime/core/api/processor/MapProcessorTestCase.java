/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.message.Message.of;
import static reactor.core.Exceptions.unwrap;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MuleFatalException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import reactor.core.publisher.Mono;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class MapProcessorTestCase extends AbstractMuleContextTestCase {

  @Mock
  private EventContext eventContext;

  private Event event = Event.builder(eventContext).message(Message.of(TEST_PAYLOAD)).build();

  private RuntimeException exception = new RuntimeException() {};

  private Error error = new LinkageError();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private Processor testProcessor = event -> Event.builder(eventContext).message(of(TEST_PAYLOAD)).build();
  private Processor testProcessorReturnsNull = event -> Event.builder(eventContext).message(of(null)).build();
  private Processor testProcessorThrowsException = event -> {
    throw exception;
  };
  private Processor testProcessorThrowsError = event -> {
    throw error;
  };

  @Test
  public void mapBlocking() throws Exception {
    Event result = testProcessor.process(event);
    assertThat(result.getMessage().getPayload().getValue(), equalTo(TEST_PAYLOAD));
  }

  @Test
  public void mapStreamBlockingGet() {
    Event result = just(event).transform(testProcessor).block();
    assertThat(result.getMessage().getPayload().getValue(), equalTo(TEST_PAYLOAD));
  }

  @Test
  public void mapStreamSubscribe() throws Exception {
    Event result = just(event).transform(testProcessor).block();
    assertThat(result.getMessage().getPayload().getValue(), equalTo(TEST_PAYLOAD));
  }

  @Test
  public void mapBlockingNullResult() throws Exception {
    Event result = testProcessorReturnsNull.process(event);
    assertThat(result.getMessage().getPayload().getValue(), is(nullValue()));
  }

  @Test
  public void mapStreamBlockingGetNullResult() {
    Event result = just(event).transform(testProcessorReturnsNull).block();
    assertThat(result.getMessage().getPayload().getValue(), is(nullValue()));
  }

  @Test
  public void mapStreamSubscribeNullResult() throws Exception {
    Event result = just(event).transform(testProcessorReturnsNull).block();
    assertThat(result.getMessage().getPayload().getValue(), is(nullValue()));
  }

  @Test
  public void mapBlockingExceptionThrown() throws Exception {
    thrown.expect(is(exception));
    testProcessorThrowsException.process(event);
  }

  @Test
  public void mapStreamBlockingGetExceptionThrown() throws Throwable {
    thrown.expect(is(instanceOf(MessagingException.class)));
    thrown.expectCause(is(exception));
    Event result;
    try {
      result = just(event).transform(testProcessorThrowsException).block();
    } catch (Exception e) {
      throw unwrap(e);
    }
    assertThat(result, is(nullValue()));
  }

  @Test
  public void mapStreamSubscribeExceptionThrown() throws Exception {
    just(event).transform(testProcessorThrowsException).onErrorResume(throwable -> {
      assertThat(throwable, is(instanceOf(MessagingException.class)));
      assertThat(throwable.getCause(), is(exception));

      // If there are no assertion errors, the actual throwable will be ignored
      return Mono.empty();
    }).subscribe();
  }

  @Test
  public void mapBlockingErrorThrown() throws Exception {
    thrown.expect(is(error));
    testProcessorThrowsError.process(event);
  }

  @Test
  public void mapStreamBlockingGetErrorThrown() throws Throwable {
    Event result = null;
    try {
      result = just(event).transform(testProcessorThrowsError).block();
    } catch (Exception e) {
      Throwable problem = unwrap(e);

      assertThat(problem, is(instanceOf(MessagingException.class)));
      assertThat(problem.getCause(), is(instanceOf(MuleFatalException.class)));
      assertThat(problem.getCause().getCause(), is(error));
    }

    assertThat(result, is(nullValue()));
  }

  @Test
  public void mapStreamSubscribeErrorThrown() throws Exception {
    just(event).transform(testProcessorThrowsError).onErrorResume(throwable -> {
      assertThat(throwable, is(instanceOf(MessagingException.class)));
      assertThat(throwable.getCause(), is(instanceOf(MuleFatalException.class)));
      assertThat(throwable.getCause().getCause(), is(error));

      // If there are no assertion errors, the actual throwable will be ignored
      return Mono.empty();
    }).subscribe().block();
  }
}
