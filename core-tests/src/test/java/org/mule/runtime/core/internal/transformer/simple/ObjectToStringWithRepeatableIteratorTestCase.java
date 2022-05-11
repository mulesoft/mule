/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.config.MuleRuntimeFeature.TO_STRING_TRANSFORMER_TRANSFORM_ITERATOR_ELEMENTS;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.streaming.object.ListCursorIterator;
import org.mule.tck.core.transformer.AbstractTransformerTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;

import io.qameta.allure.Issue;

@Issue("MULE-19323")
public class ObjectToStringWithRepeatableIteratorTestCase extends AbstractTransformerTestCase {

  private FeatureFlaggingService featureFlaggingService;

  @Before
  public void setUp() {
    featureFlaggingService = mock(FeatureFlaggingService.class);
    when(featureFlaggingService.isEnabled(TO_STRING_TRANSFORMER_TRANSFORM_ITERATOR_ELEMENTS)).thenReturn(true);
  }

  @Override
  public Transformer getTransformer() throws Exception {
    final ObjectToString objectToString = new ObjectToString();
    initialiseIfNeeded(objectToString, muleContext);
    objectToString.setFeatureFlags(featureFlaggingService);
    return objectToString;
  }

  @Override
  public Object getTestData() {
    List<String> list = new ArrayList<>();
    list.add("one");
    list.add(null);
    list.add("three");

    final CursorIteratorProvider iteratorProvider = mock(CursorIteratorProvider.class);
    when(iteratorProvider.openCursor()).thenReturn(new ListCursorIterator<>(iteratorProvider, list));

    return iteratorProvider;
  }

  @Override
  public Object getResultData() {
    return "[one, null, three]";
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    // we do not want round trip transforming tested
    return null;
  }
}
