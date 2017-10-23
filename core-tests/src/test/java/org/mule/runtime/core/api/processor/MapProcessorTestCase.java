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

import org.mule.runtime.api.exception.MuleFatalException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
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
  private BaseEventContext eventContext;

  private CoreEvent event = CoreEvent.builder(eventContext).message(Message.of(TEST_PAYLOAD)).build();

  private RuntimeException exception = new RuntimeException() {};

  private Error error = new LinkageError();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private Processor testProcessor = event -> CoreEvent.builder(eventContext).message(of(TEST_PAYLOAD)).build();
  private Processor testProcessorReturnsNull = event -> CoreEvent.builder(eventContext).message(of(null)).build();
  private Processor testProcessorThrowsException = event -> {
    throw exception;
  };
  private Processor testProcessorThrowsError = event -> {
    throw error;
  };

  @Test
  public void mapBlocking() throws Exception {
    CoreEvent result = testProcessor.process(event);
    assertThat(result.getMessage().getPayload().getValue(), equalTo(TEST_PAYLOAD));
  }

  @Test
  public void mapStreamBlockingGet() {
    CoreEvent result = just(event).transform(testProcessor).block();
    assertThat(result.getMessage().getPayload().getValue(), equalTo(TEST_PAYLOAD));
  }

  @Test
  public void mapStreamSubscribe() throws Exception {
    CoreEvent result = just(event).transform(testProcessor).block();
    assertThat(result.getMessage().getPayload().getValue(), equalTo(TEST_PAYLOAD));
  }

  @Test
  public void mapBlockingNullResult() throws Exception {
    CoreEvent result = testProcessorReturnsNull.process(event);
    assertThat(result.getMessage().getPayload().getValue(), is(nullValue()));
  }

  @Test
  public void mapStreamBlockingGetNullResult() {
    CoreEvent result = just(event).transform(testProcessorReturnsNull).block();
    assertThat(result.getMessage().getPayload().getValue(), is(nullValue()));
  }

  @Test
  public void mapStreamSubscribeNullResult() throws Exception {
    CoreEvent result = just(event).transform(testProcessorReturnsNull).block();
    assertThat(result.getMessage().getPayload().getValue(), is(nullValue()));
  }

  @Test
  public void mapBlockingExceptionThrown() throws Exception {
    thrown.expect(is(exception));
    testProcessorThrowsException.process(event);
  }

  @Test
  public void mapStreamBlockingGetExceptionThrown() throws Throwable {
    thrown.expect(is(exception));
    CoreEvent result;
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
      assertThat(throwable, is(exception));

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
    CoreEvent result = null;
    try {
      result = just(event).transform(testProcessorThrowsError).block();
    } catch (Exception e) {
      Throwable problem = unwrap(e);

      assertThat(problem, is(instanceOf(MuleFatalException.class)));
      assertThat(problem.getCause(), is(error));
    }

    assertThat(result, is(nullValue()));
  }

  @Test
  public void mapStreamSubscribeErrorThrown() throws Exception {
    just(event).transform(testProcessorThrowsError).onErrorResume(throwable -> {
      assertThat(throwable, is(instanceOf(MuleFatalException.class)));
      assertThat(throwable.getCause(), is(error));

      // If there are no assertion errors, the actual throwable will be ignored
      return Mono.empty();
    }).toProcessor().block();
  }
}
