/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.MimeType.APPLICATION_XML;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MimeType;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.expression.ExpressionManager;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import javax.activation.MimeTypeParseException;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public abstract class AbstractAddVariablePropertyTransformerTestCase extends AbstractMuleTestCase
{
    public static final String ENCODING = "encoding";
    public static final String PLAIN_STRING_KEY = "someText";
    public static final String PLAIN_STRING_VALUE = "someValue";
    public static final String EXPRESSION = "#[string:someValue]";
    public static final String EXPRESSION_VALUE = "expressionValueResult";
    public static final String NULL_EXPRESSION = "#[string:someValueNull]";
    public static final String CUSTOM_ENCODING = UTF_8.name();

    private MuleEvent mockEvent;
    private MuleMessage mockMessage;
    private MuleSession mockSession = mock(MuleSession.class);
    private MuleContext mockMuleContext = mock(MuleContext.class);
    private ExpressionManager mockExpressionManager = mock(ExpressionManager.class);
    private AbstractAddVariablePropertyTransformer addVariableTransformer;

    public AbstractAddVariablePropertyTransformerTestCase(AbstractAddVariablePropertyTransformer abstractAddVariableTransformer)
    {
        addVariableTransformer = abstractAddVariableTransformer;
    }

    @Before
    public void setUpTest() throws MimeTypeParseException
    {
        addVariableTransformer.setReturnDataType(DataType.OBJECT);

        when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
        when(mockMuleContext.getConfiguration()).thenReturn(mock(MuleConfiguration.class));
        when(mockExpressionManager.parse(anyString(), any(MuleEvent.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
        when(mockExpressionManager.evaluate(eq(EXPRESSION), any(MuleEvent.class))).thenReturn(EXPRESSION_VALUE);
        TypedValue typedValue = new TypedValue(EXPRESSION_VALUE, DataType.STRING);
        when(mockExpressionManager.evaluateTyped(eq(EXPRESSION), any(MuleEvent.class))).thenReturn(typedValue);
        addVariableTransformer.setMuleContext(mockMuleContext);

        mockMessage = new DefaultMuleMessage("", mockMuleContext);
        mockEvent = new DefaultMuleEvent(mockMessage, (FlowConstruct) null, mockSession);
    }

    @Test
    public void testAddVariable() throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        addVariableTransformer.setValue(PLAIN_STRING_VALUE);
        addVariableTransformer.initialise();
        addVariableTransformer.transform(mockEvent, ENCODING);

        verifyAdded(mockEvent, PLAIN_STRING_KEY, PLAIN_STRING_VALUE);
        assertThat(getVariableDataType(mockEvent, PLAIN_STRING_KEY), like(String.class, MimeType.ANY, null));
    }

    @Test
    public void testAddVariableWithExpressionValue() throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        addVariableTransformer.setValue(EXPRESSION);
        addVariableTransformer.initialise();
        addVariableTransformer.transform(mockEvent, ENCODING);

        verifyAdded(mockEvent, PLAIN_STRING_KEY, EXPRESSION_VALUE);
        assertThat(getVariableDataType(mockEvent, PLAIN_STRING_KEY), like(String.class, MimeType.ANY, null));
    }

    @Test
    public void testAddVariableWithExpressionKey() throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(EXPRESSION);
        addVariableTransformer.setValue(PLAIN_STRING_VALUE);
        addVariableTransformer.initialise();
        addVariableTransformer.transform(mockEvent, ENCODING);

        verifyAdded(mockEvent, EXPRESSION_VALUE, PLAIN_STRING_VALUE);
        assertThat(getVariableDataType(mockEvent, EXPRESSION_VALUE), like(String.class, MimeType.ANY, null));
    }

    @Test
    public void testAddVariableWithEncoding() throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        addVariableTransformer.setValue(PLAIN_STRING_VALUE);
        addVariableTransformer.initialise();
        addVariableTransformer.setReturnDataType(DataType.builder().encoding(CUSTOM_ENCODING).build());
        addVariableTransformer.transform(mockEvent, ENCODING);

        verifyAdded(mockEvent, PLAIN_STRING_KEY, PLAIN_STRING_VALUE);
        assertThat(getVariableDataType(mockEvent, PLAIN_STRING_KEY), like(String.class, MimeType.ANY, CUSTOM_ENCODING));
    }

    @Test
    public void testAddVariableWithMimeType() throws InitialisationException, TransformerException, MimeTypeParseException
    {
        addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        addVariableTransformer.setValue(PLAIN_STRING_VALUE);
        addVariableTransformer.initialise();
        addVariableTransformer.setReturnDataType(DataType.builder().mimeType(APPLICATION_XML).build());
        addVariableTransformer.transform(mockEvent, ENCODING);

        verifyAdded(mockEvent, PLAIN_STRING_KEY, PLAIN_STRING_VALUE);
        assertThat(getVariableDataType(mockEvent, PLAIN_STRING_KEY), like(String.class, APPLICATION_XML, null));
    }

    protected abstract DataType getVariableDataType(MuleEvent event, String key);

    @Test(expected = IllegalArgumentException.class)
    public void testAddVariableWithNullKey() throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddVariableWithEmptyKey() throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddVariableWithNullValue() throws InitialisationException, TransformerException
    {
        addVariableTransformer.setValue(null);
    }

    @Test
    public void testAddVariableWithNullExpressionKeyResult()
            throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(NULL_EXPRESSION);
        addVariableTransformer.setValue(PLAIN_STRING_VALUE);
        addVariableTransformer.initialise();
        addVariableTransformer.transform(mockEvent, ENCODING);
        verifyNotAdded(mockEvent);
    }

    @Test
    public void testAddVariableWithNullExpressionValueResult()
            throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        TypedValue typedValue = new TypedValue(null, DataType.OBJECT);
        when(mockExpressionManager.evaluateTyped(NULL_EXPRESSION, mockEvent)).thenReturn(typedValue);
        addVariableTransformer.setValue(NULL_EXPRESSION);
        addVariableTransformer.initialise();
        addVariableTransformer.transform(mockEvent, ENCODING);
        verifyRemoved(mockEvent, PLAIN_STRING_KEY);
    }

    @Test
    public void testAddVariableWithNullPayloadExpressionValueResult()
            throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        addVariableTransformer.setValue(EXPRESSION);
        TypedValue typedValue = new TypedValue(null, DataType.OBJECT);
        when(mockExpressionManager.evaluateTyped(EXPRESSION, mockEvent)).thenReturn(typedValue);
        addVariableTransformer.initialise();

        addVariableTransformer.transform(mockEvent, ENCODING);

        verifyRemoved(mockEvent, PLAIN_STRING_KEY);
    }

    protected abstract void verifyAdded(MuleEvent event, String key, String value);

    protected abstract void verifyNotAdded(MuleEvent mockEvent);

    protected abstract void verifyRemoved(MuleEvent mockEvent, String key);

}
