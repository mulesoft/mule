/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.context;

import static org.mule.runtime.tracer.impl.context.extractor.w3c.TraceParentContextFieldExtractor.TRACEPARENT;
import static org.mule.runtime.tracer.impl.context.extractor.w3c.TraceStateContextFieldExtractor.TRACESTATE;
import static org.mule.runtime.tracer.impl.span.InternalSpan.getAsInternalSpan;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EVENT_CONTEXT;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EventContextStory.DISTRIBUTED_TRACE_CONTEXT;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.mule.runtime.core.internal.profiling.tracing.event.span.condition.SpanNameAssertion;
import org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter;
import org.mule.runtime.tracer.impl.span.InternalSpan;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.api.span.validation.AssertionFailedException;

import java.util.Map;
import java.util.Optional;

@Feature(EVENT_CONTEXT)
@Story(DISTRIBUTED_TRACE_CONTEXT)
public class EventSpanContextTestCase {

  public static final String TRACEPARENT_VALUE = "TRACEPARENT_VALUE";
  public static final String TRACESTATE_VALUE = "TRACESTATE_VALUE";

  public static final String ANOTHER_KEY = "anotherKey";
  public static final String ANOTHER_FIELD_VALUE = "anotherFieldValue";

  @Test
  public void eventSpanContextBuilder() {
    DistributedTraceContextGetter distributedTraceContextGetter = mock(DistributedTraceContextGetter.class);
    when(distributedTraceContextGetter.get(any(String.class))).thenReturn(empty());
    when(distributedTraceContextGetter.get(TRACEPARENT)).thenReturn(of(TRACEPARENT_VALUE));
    when(distributedTraceContextGetter.get(TRACESTATE)).thenReturn(of(TRACESTATE_VALUE));

    // This shouldn't be considered in the serialization.
    when(distributedTraceContextGetter.get(ANOTHER_KEY)).thenReturn(of(ANOTHER_FIELD_VALUE));

    EventSpanContext spanContext =
        EventSpanContext.builder().withGetter(distributedTraceContextGetter).build();

    Optional<InternalSpan> currentSpan = Optional.ofNullable(getAsInternalSpan(spanContext.getSpan().orElse(null)));

    if (!currentSpan.isPresent()) {
      fail("No current span created");
    }

    Map<String, String> serializeAsMap = currentSpan.get().serializeAsMap();
    assertThat(serializeAsMap, aMapWithSize(2));
    assertThat(serializeAsMap, Matchers.hasEntry(TRACEPARENT, TRACEPARENT_VALUE));
    assertThat(serializeAsMap, Matchers.hasEntry(TRACESTATE, TRACESTATE_VALUE));
  }

  @Test(expected = AssertionFailedException.class)
  public void testEndSpanUnsuccessfulAssertion() {
    InternalSpan mockedSpan = mock(InternalSpan.class);
    when(mockedSpan.getName()).thenReturn("mockedSpan");

    DistributedTraceContextGetter distributedTraceContextGetter = mock(DistributedTraceContextGetter.class);
    when(distributedTraceContextGetter.get(any(String.class))).thenReturn(empty());
    EventSpanContext spanContext =
        EventSpanContext.builder().withGetter(distributedTraceContextGetter).build();

    spanContext.setSpan(mockedSpan, Assertion.SUCCESSFUL_ASSERTION);
    spanContext.endSpan(new SpanNameAssertion("thisShouldFail"));
  }

  @Test
  public void testEndSpanSuccessfulAssertion() {
    InternalSpan mockedSpan = mock(InternalSpan.class);
    when(mockedSpan.getName()).thenReturn("mockedSpan");

    DistributedTraceContextGetter distributedTraceContextGetter = mock(DistributedTraceContextGetter.class);
    when(distributedTraceContextGetter.get(any(String.class))).thenReturn(empty());
    EventSpanContext spanContext =
        EventSpanContext.builder().withGetter(distributedTraceContextGetter).build();

    spanContext.setSpan(mockedSpan, Assertion.SUCCESSFUL_ASSERTION);
    spanContext.endSpan(new SpanNameAssertion("mockedSpan"));
  }

  @Test(expected = AssertionFailedException.class)
  public void testStartSpanUnsuccessfulAssertion() {
    InternalSpan mockedSpan = mock(InternalSpan.class);
    when(mockedSpan.getName()).thenReturn("mockedSpan");

    DistributedTraceContextGetter distributedTraceContextGetter = mock(DistributedTraceContextGetter.class);
    when(distributedTraceContextGetter.get(any(String.class))).thenReturn(empty());
    EventSpanContext spanContext =
        EventSpanContext.builder().withGetter(distributedTraceContextGetter).build();

    spanContext.setSpan(mockedSpan, new SpanNameAssertion("thisShouldFail"));
  }

  @Test
  public void testStartSpanSuccessfulAssertion() {
    InternalSpan mockedSpan = mock(InternalSpan.class);
    when(mockedSpan.getName()).thenReturn("mockedSpan");

    DistributedTraceContextGetter distributedTraceContextGetter = mock(DistributedTraceContextGetter.class);
    when(distributedTraceContextGetter.get(any(String.class))).thenReturn(empty());
    EventSpanContext spanContext =
        EventSpanContext.builder().withGetter(distributedTraceContextGetter).build();

    spanContext.setSpan(mockedSpan, Assertion.SUCCESSFUL_ASSERTION);
    spanContext.endSpan(new SpanNameAssertion("mockedSpan"));
  }

}
