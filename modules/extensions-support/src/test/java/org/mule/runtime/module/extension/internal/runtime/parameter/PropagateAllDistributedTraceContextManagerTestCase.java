/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.runtime.parameter;

import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EVENT_CONTEXT;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EventContextStory.DISTRIBUTED_TRACE_CONTEXT;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.internal.trace.DistributedTraceContext;

import java.util.Map;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(EVENT_CONTEXT)
@Story(DISTRIBUTED_TRACE_CONTEXT)
public class PropagateAllDistributedTraceContextManagerTestCase {


  public static final String TRACE_FIELD_1 = "traceField1";
  public static final String TRACE_FIELD_VALUE_1 = "traceFieldValue1";
  public static final String TRACE_FIELD_2 = "traceField2";
  public static final String TRACE_FIELD_VALUE_2 = "traceFieldValue2";
  public static final String TRACE_FIELD_3 = "traceField3";
  public static final String TRACE_FIELD_VALUE_3 = "traceFieldValue3";
  public static final String TRACE_FIELD_4 = "traceField4";
  public static final String TRACE_FIELD_VALUE_4 = "traceFieldValue4";

  @Test
  public void propagatesAllFieldsInBaggageAndTraceFields() {
    DistributedTraceContext distributedTraceContext = mock(DistributedTraceContext.class);
    when(distributedTraceContext.tracingFieldsAsMap())
        .thenReturn(of(TRACE_FIELD_1, TRACE_FIELD_VALUE_1, TRACE_FIELD_2, TRACE_FIELD_VALUE_2));
    when(distributedTraceContext.baggageItemsAsMap())
        .thenReturn(of(TRACE_FIELD_3, TRACE_FIELD_VALUE_3, TRACE_FIELD_4, TRACE_FIELD_VALUE_4));
    PropagateAllDistributedTraceContextManager manager = new PropagateAllDistributedTraceContextManager(distributedTraceContext);

    Map<String, String> remoteContextMap = manager.getRemoteTraceContextMap();

    assertThat(remoteContextMap, hasEntry(TRACE_FIELD_1, TRACE_FIELD_VALUE_1));
    assertThat(remoteContextMap, hasEntry(TRACE_FIELD_2, TRACE_FIELD_VALUE_2));
    assertThat(remoteContextMap, hasEntry(TRACE_FIELD_3, TRACE_FIELD_VALUE_3));
    assertThat(remoteContextMap, hasEntry(TRACE_FIELD_4, TRACE_FIELD_VALUE_4));
  }

}
