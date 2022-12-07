/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.org.mule.runtime.tracer.impl.context.extractor;

import static org.mule.test.allure.AllureConstants.EventContextFeature.EVENT_CONTEXT;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EventContextStory.DISTRIBUTED_TRACE_CONTEXT;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter;
import org.mule.runtime.tracer.impl.context.extractor.ComposedTraceContextFieldExtractor;
import org.mule.runtime.tracer.impl.context.extractor.TraceContextFieldExtractor;

import java.util.HashMap;
import java.util.Map;

@Feature(EVENT_CONTEXT)
@Story(DISTRIBUTED_TRACE_CONTEXT)
public class ComposedTraceContextFieldExtractorTestCase {

  private static final String TEST_TRACE_FIELD_KEY_1 = "TEST_TRACE_FIELD_KEY_1";
  private static final String TEST_TRACE_FIELD_VALUE_1 = "TEST_TRACE_FIELD_VALUE_1";

  private static final String TEST_TRACE_FIELD_KEY_2 = "TEST_TRACE_FIELD_KEY_2";
  private static final String TEST_TRACE_FIELD_VALUE_2 = "TEST_TRACE_FIELD_VALUE_2";

  private static final String TEST_TRACE_FIELD_KEY_NOT_EXTRACTED = "TEST_TRACE_FIELD_NOT_EXTRACTED";
  private static final String TEST_TRACE_FIELD_VALUE_NOT_EXTRACTED = "TEST_TRACE_FIELD_VALUE_NOT_EXTRACTED";


  @Test
  public void composedTraceContextFieldsExtractorComposesAndOnlyRetrievesTheComposedExtractorFields() {
    DistributedTraceContextGetter distributedTraceContextMapGetter = mock(DistributedTraceContextGetter.class);
    when(distributedTraceContextMapGetter.get(any(String.class))).thenReturn(empty());

    when(distributedTraceContextMapGetter.get(TEST_TRACE_FIELD_KEY_1)).thenReturn(of(TEST_TRACE_FIELD_VALUE_1));
    when(distributedTraceContextMapGetter.get(TEST_TRACE_FIELD_KEY_2)).thenReturn(of(TEST_TRACE_FIELD_VALUE_2));
    when(distributedTraceContextMapGetter.get(TEST_TRACE_FIELD_KEY_NOT_EXTRACTED))
        .thenReturn(of(TEST_TRACE_FIELD_VALUE_NOT_EXTRACTED));

    ComposedTraceContextFieldExtractor composedTraceContextFieldExtractor =
        new ComposedTraceContextFieldExtractor(new TestTraceContextExtractor(TEST_TRACE_FIELD_KEY_1),
                                               new TestTraceContextExtractor(TEST_TRACE_FIELD_KEY_2));

    Map<String, String> extractedValuesMap = composedTraceContextFieldExtractor.extract(distributedTraceContextMapGetter);
    assertThat(extractedValuesMap, aMapWithSize(2));
    assertThat(extractedValuesMap, hasEntry(equalTo(TEST_TRACE_FIELD_KEY_1), equalTo(TEST_TRACE_FIELD_VALUE_1)));
    assertThat(extractedValuesMap, hasEntry(equalTo(TEST_TRACE_FIELD_KEY_2), equalTo(TEST_TRACE_FIELD_VALUE_2)));
  }

  /**
   * Test {@link TraceContextFieldExtractor}
   */
  private static class TestTraceContextExtractor implements TraceContextFieldExtractor {

    private final String key;

    public TestTraceContextExtractor(String key) {
      this.key = key;
    }

    @Override
    public Map<String, String> extract(DistributedTraceContextGetter distributedTraceContextMapGetter) {
      Map<String, String> resultMap = new HashMap<>();
      if (distributedTraceContextMapGetter.get(key).isPresent()) {
        resultMap.put(key, distributedTraceContextMapGetter.get(key).get());
      }
      return resultMap;
    }
  }
}
