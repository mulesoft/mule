/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.expression;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import static org.mule.transformer.types.MimeTypes.ANY;
import static org.mule.transformer.types.MimeTypes.JSON;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.TypedValue;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class MessagePayloadExpressionEvaluatorTestCase extends AbstractMuleTestCase
{

    private static final String CUSTOM_ENCODING = StandardCharsets.UTF_16.name();
    private static final String PAYLOAD = "test";

    @Test
    public void evaluatesWithType() throws Exception
    {
        MessagePayloadExpressionEvaluator evaluator = new MessagePayloadExpressionEvaluator();


        final DataType dataType = DataTypeFactory.create(String.class, JSON);
        dataType.setEncoding(CUSTOM_ENCODING);

        final MuleMessage message = mock(MuleMessage.class);
        when(message.getPayload()).thenReturn(PAYLOAD);
        when(message.getDataType()).thenReturn(dataType);

        final TypedValue typedValue = evaluator.evaluateTyped("", message);

        assertThat((String) typedValue.getValue(), equalTo("test"));
        assertThat(typedValue.getDataType(), like(String.class, JSON, CUSTOM_ENCODING));
    }

    @Test
    public void evaluatesNullMessageWithType() throws Exception
    {
        MessagePayloadExpressionEvaluator evaluator = new MessagePayloadExpressionEvaluator();

        final DataType dataType = DataTypeFactory.create(String.class, JSON);
        dataType.setEncoding(CUSTOM_ENCODING);

        final TypedValue typedValue = evaluator.evaluateTyped("", null);

        assertThat(typedValue.getValue(), is(nullValue()));
        assertThat(typedValue.getDataType(), like(Object.class, ANY, null));
    }
}