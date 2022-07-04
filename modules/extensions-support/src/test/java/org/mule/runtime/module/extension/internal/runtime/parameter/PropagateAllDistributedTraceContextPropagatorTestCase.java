/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.runtime.parameter;

import static org.mule.test.allure.AllureConstants.EventContextFeature.EVENT_CONTEXT;
import static org.mule.test.allure.AllureConstants.EventContextFeature.EventContextStory.DISTRIBUTED_TRACE_CONTEXT;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import org.mule.runtime.core.internal.trace.DistributedTraceContext;
import org.mule.runtime.extension.api.runtime.parameter.DistributedTraceContextSetter;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(EVENT_CONTEXT)
@Story(DISTRIBUTED_TRACE_CONTEXT)
public class PropagateAllDistributedTraceContextPropagatorTestCase {

  public static final String KEY_1 = "KEY1";
  public static final String VALUE_1 = "VALUE1";
  public static final String KEY_2 = "KEY2";
  public static final String VALUE_2 = "VALUE2";
  public static final String KEY_3 = "KEY3";
  public static final String VALUE_3 = "VALUE3";
  public static final String KEY_4 = "KEY4";
  public static final String VALUE_4 = "VALUE4";

  @Test
  public void allFieldArePropagated() {
    DistributedTraceContext distributedTraceContext = mock(DistributedTraceContext.class);
    Map<String, String> traceFields = ImmutableMap.of(KEY_3, VALUE_3, KEY_4, VALUE_4);
    Map<String, String> baggateItemMap = ImmutableMap.of(KEY_1, VALUE_1, KEY_2, VALUE_2);
    when(distributedTraceContext.tracingFieldsAsMap()).thenReturn(traceFields);
    when(distributedTraceContext.baggageItemsAsMap()).thenReturn(baggateItemMap);
    PropagateAllDistributedTraceContextPropagator propagator =
        new PropagateAllDistributedTraceContextPropagator(distributedTraceContext);
    DistributedTraceContextSetter setter = mock(DistributedTraceContextSetter.class);
    propagator.injectDistributedTraceFields(setter);

    verify(setter).set(KEY_1, VALUE_1);
    verify(setter).set(KEY_2, VALUE_2);
    verify(setter).set(KEY_3, VALUE_3);
    verify(setter).set(KEY_4, VALUE_4);
  }
}
