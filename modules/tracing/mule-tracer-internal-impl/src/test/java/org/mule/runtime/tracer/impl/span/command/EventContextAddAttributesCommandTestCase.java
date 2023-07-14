/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.tracer.impl.span.command.EventContextAddAttributesCommand.getEventContextAddAttributesCommand;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.util.Optional.of;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.api.profiling.tracing.SpanIdentifier;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.api.span.InternalSpan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.runtime.tracer.api.span.SpanAttribute;
import org.mule.runtime.tracer.api.span.StringSpanAttribute;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.slf4j.Logger;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class EventContextAddAttributesCommandTestCase {

  public static final String ATTRIBUTE_KEY_1 = "ATTRIBUTE_KEY_1";
  public static final String ATTRIBUTE_VALUE_1 = "ATTRIBUTE_VALUE_1";
  public static final String ATTRIBUTE_KEY_2 = "ATTRIBUTE_KEY_2";
  public static final String ATTRIBUTE_VALUE_2 = "ATTRIBUTE_VALUE_2";
  public static final String TEST_ERROR_MESSAGE = "Test error";

  @Test
  public void verifyAttributesAreAdded() {
    SpanContextAware eventContext = mock(SpanContextAware.class, withSettings().extraInterfaces(EventContext.class));

    SpanContext spanContext = mock(SpanContext.class);
    when(eventContext.getSpanContext()).thenReturn(spanContext);

    InternalSpan span = getSpanStub();
    when(spanContext.getSpan()).thenReturn(of(span));

    List<SpanAttribute<String>> attributes = new ArrayList<>();
    attributes.add(new StringSpanAttribute(ATTRIBUTE_KEY_1, ATTRIBUTE_VALUE_1));
    attributes.add(new StringSpanAttribute(ATTRIBUTE_KEY_2, ATTRIBUTE_VALUE_2));

    EventContextAddAttributesCommand addAttributeCommand =
        getEventContextAddAttributesCommand(mock(Logger.class), TEST_ERROR_MESSAGE, true);

    addAttributeCommand.execute((EventContext) eventContext, attributes);


    assertThat(span.getAttributesCount(), equalTo(2));

    span.forEachAttribute((s, s2) -> {
      boolean isExpectedAttribute = (s.equals(ATTRIBUTE_KEY_1) && s2.equals(ATTRIBUTE_VALUE_1))
          || (s.equals(ATTRIBUTE_KEY_2) && s2.equals(ATTRIBUTE_VALUE_2));
      assertThat(isExpectedAttribute, is(true));
    });
  }

  private static InternalSpan getSpanStub() {
    return new InternalSpan() {

      private final List<SpanAttribute<String>> spanAttributes = new ArrayList<>();

      @Override
      public void addAttribute(SpanAttribute<String> spanAttribute) {
        this.spanAttributes.add(spanAttribute);
      }

      @Override
      public void end() {

      }

      @Override
      public void end(long endTime) {

      }

      @Override
      public void addError(InternalSpanError error) {

      }

      @Override
      public void updateName(String name) {

      }

      @Override
      public void forEachAttribute(BiConsumer<String, String> biConsumer) {
        spanAttributes.forEach(spanAttribute -> biConsumer.accept(spanAttribute.getKey(), spanAttribute.getValue()));
      }

      @Override
      public Map<String, String> serializeAsMap() {
        return null;
      }

      @Override
      public int getAttributesCount() {
        return 0;
      }

      @Override
      public InternalSpan onChild(InternalSpan child) {
        return null;
      }

      @Override
      public Span getParent() {
        return null;
      }

      @Override
      public SpanIdentifier getIdentifier() {
        return null;
      }

      @Override
      public String getName() {
        return null;
      }

      @Override
      public SpanDuration getDuration() {
        return null;
      }

      @Override
      public List<SpanError> getErrors() {
        return null;
      }

      @Override
      public boolean hasErrors() {
        return false;
      }
    };
  }
}
