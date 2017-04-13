/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.message.Message.of;
import static reactor.core.Exceptions.unwrap;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class MapProcessorTestCase extends AbstractMuleContextTestCase {

  @Mock
  private EventContext eventContext;

  @Mock
  private Event event;

  private RuntimeException exception = new RuntimeException() {};

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private Processor testProcessor = event -> Event.builder(eventContext).message(of(TEST_PAYLOAD)).build();
  private Processor testProcessorReturnsNull = event -> Event.builder(eventContext).message(of(null)).build();
  private Processor testProcessorThrowsException = event -> {
    throw exception;
  };

  @Test
  public void mapBlocking() throws Exception {
    assertThat(testProcessor.process(event).getMessage().getPayload().getValue(), equalTo(TEST_PAYLOAD));
  }

  @Test
  public void mapStreamBlockingGet() {
    assertThat(just(event).transform(testProcessor).block().getMessage().getPayload().getValue(),
               equalTo(TEST_PAYLOAD));
  }

  @Test
  public void mapStreamSubscribe() throws Exception {
    Latch latch = new Latch();
    just(event).transform(testProcessor).subscribe(event -> {
      assertThat(event.getMessage().getPayload().getValue(), equalTo(TEST_PAYLOAD));
      latch.countDown();
    });
    latch.await(RECEIVE_TIMEOUT, MILLISECONDS);
  }

  @Test
  public void mapBlockingNullResult() throws Exception {
    assertThat(testProcessorReturnsNull.process(event).getMessage().getPayload().getValue(), is(nullValue()));
  }

  @Test
  public void mapStreamBlockingGetNullResult() {
    assertThat(just(event).transform(testProcessorReturnsNull).block().getMessage().getPayload().getValue(), is(nullValue()));
  }

  @Test
  public void mapStreamSubscribeNullResult() throws Exception {
    Latch latch = new Latch();
    just(event).transform(testProcessorReturnsNull).subscribe(event -> {
      assertThat(event.getMessage().getPayload().getValue(), is(nullValue()));
      latch.countDown();
    });
    latch.await(RECEIVE_TIMEOUT, MILLISECONDS);
  }

  @Test
  public void mapBlockingExceptionThrown() throws Exception {
    thrown.expect(is(exception));
    assertThat(testProcessorThrowsException.process(event), is(nullValue()));
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
    Latch latch = new Latch();
    just(event).transform(testProcessorThrowsException).doOnError(throwable -> {
      assertThat(throwable, is(instanceOf(MessagingException.class)));
      assertThat(throwable.getCause(), is(exception));
      latch.countDown();
    }).subscribe();
    latch.await(RECEIVE_TIMEOUT, MILLISECONDS);
  }

}
