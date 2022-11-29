/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.org.mule.runtime.tracer.impl.context.extractor;

import static org.mule.runtime.tracer.impl.context.extractor.CorrelationIdTraceContextFieldExtractor.MULE_CORRELATION_ID;
import static org.mule.runtime.tracer.impl.context.extractor.CorrelationIdTraceContextFieldExtractor.X_CORRELATION_ID;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EVENT_CONTEXT;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EventContextStory.DISTRIBUTED_TRACE_CONTEXT;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter;
import org.mule.runtime.tracer.impl.context.extractor.CorrelationIdTraceContextFieldExtractor;

import java.util.Map;

@Feature(EVENT_CONTEXT)
@Story(DISTRIBUTED_TRACE_CONTEXT)
public class CorrelationIdTraceContextFieldExtractorTestCase {

  public static final String TEST_X_CORRELATION_ID = "TEST_CORRELATION_ID";
  public static final String TEST_MULE_CORRELATION_ID = "TEST_CORRELATION_ID";

  @Test
  public void noFieldsExtractedWhenFieldsNoPresentInGetter() {
    DistributedTraceContextGetter distributedTraceContextMapGetter = mock(DistributedTraceContextGetter.class);
    when(distributedTraceContextMapGetter.get(any(String.class))).thenReturn(empty());
    CorrelationIdTraceContextFieldExtractor correlationIdTraceContextFieldExtractor =
        new CorrelationIdTraceContextFieldExtractor();
    assertThat(correlationIdTraceContextFieldExtractor.extract(distributedTraceContextMapGetter), aMapWithSize(0));
  }

  @Test
  public void onlyXCorrelationIdKeyReturnsXCorrelationId() {
    testWithOnlyOneEntry(X_CORRELATION_ID, TEST_X_CORRELATION_ID);
  }

  @Test
  public void onlyMuleCorrelationIdKeyReturnsMuleCorrelationId() {
    testWithOnlyOneEntry(MULE_CORRELATION_ID, TEST_MULE_CORRELATION_ID);
  }

  @Test
  public void xCorrelationIdSupersedesMuleCorrelationId() {
    DistributedTraceContextGetter distributedTraceContextMapGetter = mock(DistributedTraceContextGetter.class);
    when(distributedTraceContextMapGetter.get(any(String.class))).thenReturn(empty());
    when(distributedTraceContextMapGetter.get(X_CORRELATION_ID)).thenReturn(of(TEST_X_CORRELATION_ID));
    when(distributedTraceContextMapGetter.get(MULE_CORRELATION_ID)).thenReturn(of(TEST_MULE_CORRELATION_ID));
    CorrelationIdTraceContextFieldExtractor correlationIdTraceContextFieldExtractor =
        new CorrelationIdTraceContextFieldExtractor();
    Map<String, String> extractedValuesMap = correlationIdTraceContextFieldExtractor.extract(distributedTraceContextMapGetter);
    assertThat(extractedValuesMap, aMapWithSize(1));
    assertThat(extractedValuesMap, hasEntry(equalTo(X_CORRELATION_ID), equalTo(TEST_X_CORRELATION_ID)));
  }

  private void testWithOnlyOneEntry(String key, String value) {
    DistributedTraceContextGetter distributedTraceContextMapGetter = mock(DistributedTraceContextGetter.class);
    when(distributedTraceContextMapGetter.get(any(String.class))).thenReturn(empty());
    when(distributedTraceContextMapGetter.get(key)).thenReturn(of(value));
    CorrelationIdTraceContextFieldExtractor correlationIdTraceContextFieldExtractor =
        new CorrelationIdTraceContextFieldExtractor();
    Map<String, String> extractedValuesMap = correlationIdTraceContextFieldExtractor.extract(distributedTraceContextMapGetter);
    assertThat(extractedValuesMap, aMapWithSize(1));
    assertThat(extractedValuesMap, hasEntry(equalTo(key), equalTo(value)));
  }

}
