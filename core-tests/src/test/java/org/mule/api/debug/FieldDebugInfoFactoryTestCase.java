/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.debug;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.tck.junit4.matcher.FieldDebugInfoMatcher.fieldLike;
import org.mule.api.MuleEvent;
import org.mule.api.debug.FieldDebugInfoFactory.FieldEvaluator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.AttributeEvaluator;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

@SmallTest
public class FieldDebugInfoFactoryTestCase extends AbstractMuleTestCase
{

    public static final Exception EVALUATION_EXCEPTION = new IllegalStateException("Error");
    public static final String FIELD_NAME = "foo";
    public static final String STRING_VALUE = "bar";

    private final MuleEvent event = mock(MuleEvent.class);
    private final AttributeEvaluator attributeEvaluator = mock(AttributeEvaluator.class);
    private final FieldEvaluator fieldEvaluator = mock(FieldEvaluator.class);


    @Test
    public void createsStringFieldWithAttributeEvaluator() throws Exception
    {
        when(attributeEvaluator.resolveStringValue(event)).thenReturn(STRING_VALUE);

        final FieldDebugInfo<?> debugInfo = FieldDebugInfoFactory.createFieldDebugInfo(FIELD_NAME, String.class, attributeEvaluator, event);

        assertCreatedFieldDebugInfo(debugInfo, String.class, STRING_VALUE, SimpleFieldDebugInfo.class);
    }

    @Test
    public void createsBooleanFieldWithAttributeEvaluator() throws Exception
    {
        when(attributeEvaluator.resolveBooleanValue(event)).thenReturn(true);

        final FieldDebugInfo<?> debugInfo = FieldDebugInfoFactory.createFieldDebugInfo(FIELD_NAME, Boolean.class, attributeEvaluator, event);

        assertCreatedFieldDebugInfo(debugInfo, Boolean.class, true, SimpleFieldDebugInfo.class);
    }

    @Test
    public void createsIntegerFieldWithAttributeEvaluator() throws Exception
    {
        when(attributeEvaluator.resolveIntegerValue(event)).thenReturn(1);

        final FieldDebugInfo<?> debugInfo = FieldDebugInfoFactory.createFieldDebugInfo(FIELD_NAME, Integer.class, attributeEvaluator, event);

        assertCreatedFieldDebugInfo(debugInfo, Integer.class, 1, SimpleFieldDebugInfo.class);
    }

    @Test
    public void createsObjectFieldWithAttributeEvaluator() throws Exception
    {
        final Object value = new Object();
        when(attributeEvaluator.resolveValue(event)).thenReturn(value);

        final FieldDebugInfo<?> debugInfo = FieldDebugInfoFactory.createFieldDebugInfo(FIELD_NAME, Object.class, attributeEvaluator, event);

        assertCreatedFieldDebugInfo(debugInfo, Object.class, value, SimpleFieldDebugInfo.class);
    }

    @Test
    public void createsStringFieldWithAttributeEvaluatorError() throws Exception
    {
        when(attributeEvaluator.resolveStringValue(event)).thenThrow(EVALUATION_EXCEPTION);

        final FieldDebugInfo<?> debugInfo = FieldDebugInfoFactory.createFieldDebugInfo(FIELD_NAME, String.class, attributeEvaluator, event);

        assertCreatedFieldDebugInfo(debugInfo, String.class, EVALUATION_EXCEPTION, ErrorFieldDebugInfo.class);
    }

    @Test
    public void createsBooleanFieldWithAttributeEvaluatorError() throws Exception
    {
        when(attributeEvaluator.resolveBooleanValue(event)).thenThrow(EVALUATION_EXCEPTION);

        final FieldDebugInfo<?> debugInfo = FieldDebugInfoFactory.createFieldDebugInfo(FIELD_NAME, Boolean.class, attributeEvaluator, event);

        assertCreatedFieldDebugInfo(debugInfo, Boolean.class, EVALUATION_EXCEPTION, ErrorFieldDebugInfo.class);
    }

    @Test
    public void createsIntegerFieldWithAttributeEvaluatorError() throws Exception
    {
        when(attributeEvaluator.resolveIntegerValue(event)).thenThrow(EVALUATION_EXCEPTION);

        final FieldDebugInfo<?> debugInfo = FieldDebugInfoFactory.createFieldDebugInfo(FIELD_NAME, Integer.class, attributeEvaluator, event);

        assertCreatedFieldDebugInfo(debugInfo, Integer.class, EVALUATION_EXCEPTION, ErrorFieldDebugInfo.class);
    }

    @Test
    public void createsObjectFieldWithAttributeEvaluatorError() throws Exception
    {
        when(attributeEvaluator.resolveValue(event)).thenThrow(EVALUATION_EXCEPTION);

        final FieldDebugInfo<?> debugInfo = FieldDebugInfoFactory.createFieldDebugInfo(FIELD_NAME, Object.class, attributeEvaluator, event);

        assertCreatedFieldDebugInfo(debugInfo, Object.class, EVALUATION_EXCEPTION, ErrorFieldDebugInfo.class);
    }

    @Test
    public void createsFieldWithFieldEvaluator() throws Exception
    {
        when(fieldEvaluator.evaluate()).thenReturn(STRING_VALUE);

        final FieldDebugInfo<?> debugInfo = FieldDebugInfoFactory.createFieldDebugInfo(FIELD_NAME, String.class, fieldEvaluator);

        assertCreatedFieldDebugInfo(debugInfo, String.class, STRING_VALUE, SimpleFieldDebugInfo.class);
    }

    @Test
    public void createsFieldWithFieldEvaluatorError() throws Exception
    {
        when(fieldEvaluator.evaluate()).thenThrow(EVALUATION_EXCEPTION);

        final FieldDebugInfo<?> debugInfo = FieldDebugInfoFactory.createFieldDebugInfo(FIELD_NAME, String.class, fieldEvaluator);
        assertCreatedFieldDebugInfo(debugInfo, String.class, EVALUATION_EXCEPTION, ErrorFieldDebugInfo.class);
    }

    @Test
    public void createsFieldWithValue() throws Exception
    {
        final FieldDebugInfo<?> debugInfo = FieldDebugInfoFactory.createFieldDebugInfo(FIELD_NAME, String.class, STRING_VALUE);

        assertCreatedFieldDebugInfo(debugInfo, String.class, STRING_VALUE, SimpleFieldDebugInfo.class);
    }

    @Test
    public void createsFieldWithError() throws Exception
    {
        final FieldDebugInfo<?> debugInfo = FieldDebugInfoFactory.createFieldDebugInfo(FIELD_NAME, String.class, EVALUATION_EXCEPTION);

        assertCreatedFieldDebugInfo(debugInfo, String.class, EVALUATION_EXCEPTION, ErrorFieldDebugInfo.class);
    }

    @Test
    public void createsFieldWithObjectDebugInfo() throws Exception
    {
        final SimpleFieldDebugInfo fieldDebugInfo = FieldDebugInfoFactory.createFieldDebugInfo(FIELD_NAME, String.class, STRING_VALUE);
        final List<FieldDebugInfo> value = Collections.<FieldDebugInfo>singletonList(fieldDebugInfo);

        final FieldDebugInfo<?> debugInfo = FieldDebugInfoFactory.createFieldDebugInfo(FIELD_NAME, Map.class, value);

        assertCreatedFieldDebugInfo(debugInfo, Map.class, value, ObjectFieldDebugInfo.class);
    }

    private void assertCreatedFieldDebugInfo(FieldDebugInfo<?> debugInfo, Class type, Object value, Class<? extends FieldDebugInfo> debugInfoClass)
    {
        assertThat(debugInfo, instanceOf(debugInfoClass));
        assertThat(debugInfo, fieldLike(FIELD_NAME, type, value));
    }
}