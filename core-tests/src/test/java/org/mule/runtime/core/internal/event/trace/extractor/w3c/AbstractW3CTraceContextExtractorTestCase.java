/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.event.trace.extractor.w3c;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.internal.event.trace.extractor.TraceContextFieldExtractor;
import org.mule.sdk.api.runtime.source.SdkDistributedTraceContextMapGetter;

import java.util.Map;

import org.junit.Test;

public abstract class AbstractW3CTraceContextExtractorTestCase {

  public static final String TEST_VALUE = "TEST_VALUE";
  public static final String ANOTHER_KEY = "ANOTHER_KEY";

  @Test
  public void noOtherFieldsApartFromTraceParent() {
    SdkDistributedTraceContextMapGetter sdkDistributedTraceContextMapGetter = mock(SdkDistributedTraceContextMapGetter.class);
    when(sdkDistributedTraceContextMapGetter.get(any(String.class))).thenReturn(empty());
    when(sdkDistributedTraceContextMapGetter.get(getTraceField())).thenReturn(of(TEST_VALUE));
    when(sdkDistributedTraceContextMapGetter.get(ANOTHER_KEY)).thenReturn(of(ANOTHER_KEY));
    Map<String, String> extractedValuesMap = getTraceContextFieldExtractor().extract(sdkDistributedTraceContextMapGetter);
    assertThat(extractedValuesMap, aMapWithSize(1));
    assertThat(extractedValuesMap, hasEntry(equalTo(getTraceField()), equalTo(TEST_VALUE)));
  }

  public abstract String getTraceField();

  public abstract TraceContextFieldExtractor getTraceContextFieldExtractor();

}
