/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.event.trace;

import static org.junit.Assert.fail;
import static org.mule.runtime.core.internal.event.trace.extractor.w3c.TraceParentContextFieldExtractor.TRACEPARENT;
import static org.mule.runtime.core.internal.event.trace.extractor.w3c.TraceStateContextFieldExtractor.TRACESTATE;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EVENT_CONTEXT;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EventContextStory.DISTRIBUTED_TRACE_CONTEXT;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.internal.trace.DistributedTraceContext;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import org.junit.Test;

@Feature(EVENT_CONTEXT)
@Story(DISTRIBUTED_TRACE_CONTEXT)
public class EventDistributedTraceContextTestCase {

  public static final String TRACEPARENT_VALUE = "TRACEPARENT_VALUE";
  public static final String TRACESTATE_VALUE = "TRACESTATE_VALUE";

  public static final String ANOTHER_KEY = "anotherKey";
  public static final String ANOTHER_FIELD_VALUE = "anotherFieldValue";

  @Test
  public void eventDistributedTraceContextBuilder() {
    DistributedTraceContextGetter distributedTraceContextGetter = mock(DistributedTraceContextGetter.class);
    when(distributedTraceContextGetter.get(any(String.class))).thenReturn(empty());
    when(distributedTraceContextGetter.get(TRACEPARENT)).thenReturn(of(TRACEPARENT_VALUE));
    when(distributedTraceContextGetter.get(TRACESTATE)).thenReturn(of(TRACESTATE_VALUE));
    when(distributedTraceContextGetter.get(ANOTHER_KEY)).thenReturn(of(ANOTHER_FIELD_VALUE));

    DistributedTraceContext eventDistributedTraceContext =
        EventDistributedTraceContext.builder().withGetter(distributedTraceContextGetter).build();

    if (!eventDistributedTraceContext.getTraceFieldValue(TRACEPARENT).isPresent()) {
      fail("No traceparent propagated!");
    }

    if (!eventDistributedTraceContext.getBaggageItem(TRACESTATE).isPresent()) {
      fail("No tracestate propagated!");
    }

    assertThat(eventDistributedTraceContext.getTraceFieldValue(TRACEPARENT).get(), equalTo(TRACEPARENT_VALUE));
    assertThat(eventDistributedTraceContext.getBaggageItem(TRACESTATE).get(), equalTo(TRACESTATE_VALUE));
    assertThat(eventDistributedTraceContext.tracingFieldsAsMap(), aMapWithSize(1));
    assertThat(eventDistributedTraceContext.baggageItemsAsMap(), aMapWithSize(1));
  }

}
