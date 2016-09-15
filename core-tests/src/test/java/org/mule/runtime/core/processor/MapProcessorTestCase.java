/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.api.message.InternalMessage.of;
import static org.mule.tck.junit4.AbstractMuleContextTestCase.RECEIVE_TIMEOUT;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class MapProcessorTestCase extends AbstractMuleTestCase {

  @Mock
  private EventContext eventContext;

  @Mock
  private Event event;

  @Test
  public void mapBlocking() throws Exception {
    Processor mapProcessor = event -> Event.builder(eventContext).message(of(TEST_PAYLOAD)).build();
    assertThat(mapProcessor.process(event).getMessage().getPayload().getValue(), equalTo(TEST_PAYLOAD));
  }

  @Test
  public void mapStreamBlockingGet() {

    Processor mapProcessor = event -> Event.builder(eventContext).message(of(TEST_PAYLOAD)).build();

    assertThat(just(event).transform(mapProcessor).block().getMessage().getPayload().getValue(),
               equalTo(TEST_PAYLOAD));
  }

  @Test
  public void mapStreamSubscribe() throws Exception {
    Processor mapProcessor = event -> Event.builder(eventContext).message(of(TEST_PAYLOAD)).build();
    Latch latch = new Latch();
    just(event).transform(mapProcessor).subscribe(event -> {
      latch.countDown();
      assertThat(event.getMessage().getPayload().getValue(), equalTo(TEST_PAYLOAD));
    });
    latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);
  }

  @Test
  public void mapBlockingNullResult() throws Exception {
    Processor mapProcessor = event -> Event.builder(eventContext).message(of(null)).build();
    assertThat(mapProcessor.process(event).getMessage().getPayload().getValue(), equalTo(null));
  }

  @Test
  public void mapStreamBlockingGetNullResult() {

    Processor mapProcessor = event -> Event.builder(eventContext).message(of(null)).build();

    assertThat(just(event).transform(mapProcessor).block().getMessage().getPayload().getValue(),
               equalTo(null));
  }

  @Test
  public void mapStreamSubscribeNullResult() throws Exception {
    Processor mapProcessor = event -> Event.builder(eventContext).message(of(null)).build();
    Latch latch = new Latch();
    just(event).transform(mapProcessor).subscribe(event -> {
      latch.countDown();
      assertThat(event.getMessage().getPayload().getValue(), equalTo(null));
    });
    latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);
  }

  @Test
  public void mapBlockingExceptionThrown() throws Exception {
    Processor mapProcessor = event1 -> {
      throw new IllegalArgumentException();
    };

    Event result = null;
    try {
      result = mapProcessor.process(event);
      fail("Exception expected");
    } catch (Exception e) {
      assertThat(e, instanceOf(IllegalArgumentException.class));
      assertThat(result, nullValue());
    }
  }

  @Test
  public void mapStreamBlockingGetExceptionThrown() {
    Processor mapProcessor = event1 -> {
      throw new IllegalArgumentException();
    };

    Event result = null;
    try {
      result = just(event).transform(mapProcessor).block();
      fail("Exception expected");
    } catch (Exception e) {
      assertThat(e, instanceOf(IllegalArgumentException.class));
      assertThat(result, nullValue());
    }
  }

  @Test
  public void mapStreamSubscribeExceptionThrown() throws Exception {
    Processor mapProcessor = event1 -> {
      throw new IllegalArgumentException();
    };
    Latch latch = new Latch();
    just(event).transform(mapProcessor).doOnError(throwable -> {
      latch.countDown();
      assertThat(throwable, instanceOf(IllegalArgumentException.class));
    }).subscribe();
    latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);
  }

}
