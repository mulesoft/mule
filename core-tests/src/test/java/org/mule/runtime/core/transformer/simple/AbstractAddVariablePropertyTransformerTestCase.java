/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.transformer.types.MimeTypes.APPLICATION_XML;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.expression.ExpressionManager;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.matcher.DataTypeMatcher;
import org.mule.tck.size.SmallTest;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.runtime.core.transformer.types.MimeTypes;
import org.mule.runtime.core.transformer.types.TypedValue;

import javax.activation.MimeTypeParseException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

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

    private MuleEvent mockEvent = mock(MuleEvent.class);
    private MuleMessage mockMessage = mock(MuleMessage.class);
    private MuleSession mockSession = mock(MuleSession.class);
    private MuleContext mockMuleContext = mock(MuleContext.class);
    private ExpressionManager mockExpressionManager = mock(ExpressionManager.class);
    private AbstractAddVariablePropertyTransformer addVariableTransformer;
    private final ArgumentCaptor<DataType> dataTypeCaptor = ArgumentCaptor.forClass(DataType.class);

    public AbstractAddVariablePropertyTransformerTestCase(AbstractAddVariablePropertyTransformer abstractAddVariableTransformer)
    {
        addVariableTransformer = abstractAddVariableTransformer;
    }

    @Before
    public void setUpTest() throws MimeTypeParseException
    {
        addVariableTransformer.setEncoding(null);
        addVariableTransformer.setMimeType(null);

        when(mockEvent.getMessage()).thenReturn(mockMessage);
        when(mockEvent.getSession()).thenReturn(mockSession);
        when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
        when(mockExpressionManager.parse(anyString(), Mockito.any(MuleEvent.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
        when(mockExpressionManager.evaluate(EXPRESSION, mockEvent)).thenReturn(EXPRESSION_VALUE);
        TypedValue typedValue = new TypedValue(EXPRESSION_VALUE, DataTypeFactory.STRING);
        when(mockExpressionManager.evaluateTyped(EXPRESSION, mockEvent)).thenReturn(typedValue);
        addVariableTransformer.setMuleContext(mockMuleContext);
    }

    @Test
    public void testAddVariable() throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        addVariableTransformer.setValue(PLAIN_STRING_VALUE);
        addVariableTransformer.initialise();
        addVariableTransformer.transform(mockEvent, ENCODING);

        verifyAdded(mockEvent, PLAIN_STRING_KEY, PLAIN_STRING_VALUE, dataTypeCaptor);
        assertThat(dataTypeCaptor.getValue(), DataTypeMatcher.like(String.class, MimeTypes.ANY, null));
    }

    @Test
    public void testAddVariableWithExpressionValue() throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        addVariableTransformer.setValue(EXPRESSION);
        addVariableTransformer.initialise();
        addVariableTransformer.transform(mockEvent, ENCODING);

        verifyAdded(mockEvent, PLAIN_STRING_KEY, EXPRESSION_VALUE, dataTypeCaptor);
        assertThat(dataTypeCaptor.getValue(), DataTypeMatcher.like(String.class, MimeTypes.ANY, null));
    }

    @Test
    public void testAddVariableWithExpressionKey() throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(EXPRESSION);
        addVariableTransformer.setValue(PLAIN_STRING_VALUE);
        addVariableTransformer.initialise();
        addVariableTransformer.transform(mockEvent, ENCODING);

        verifyAdded(mockEvent, EXPRESSION_VALUE, PLAIN_STRING_VALUE, dataTypeCaptor);
        assertThat(dataTypeCaptor.getValue(), DataTypeMatcher.like(String.class, MimeTypes.ANY, null));
    }

    @Test
    public void testAddVariableWithEncoding() throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        addVariableTransformer.setValue(PLAIN_STRING_VALUE);
        addVariableTransformer.initialise();
        addVariableTransformer.setEncoding(CUSTOM_ENCODING);
        addVariableTransformer.transform(mockEvent, ENCODING);

        verifyAdded(mockEvent, PLAIN_STRING_KEY, PLAIN_STRING_VALUE, dataTypeCaptor);
        assertThat(dataTypeCaptor.getValue(), DataTypeMatcher.like(String.class, MimeTypes.ANY, CUSTOM_ENCODING));
    }

    @Test
    public void testAddVariableWithMimeType() throws InitialisationException, TransformerException, MimeTypeParseException
    {
        addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        addVariableTransformer.setValue(PLAIN_STRING_VALUE);
        addVariableTransformer.initialise();
        addVariableTransformer.setMimeType(APPLICATION_XML);
        addVariableTransformer.transform(mockEvent, ENCODING);

        verifyAdded(mockEvent, PLAIN_STRING_KEY, PLAIN_STRING_VALUE, dataTypeCaptor);
        assertThat(dataTypeCaptor.getValue(), DataTypeMatcher.like(String.class, APPLICATION_XML, null));
    }

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
        TypedValue typedValue = new TypedValue(null, DataType.OBJECT_DATA_TYPE);
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
        TypedValue typedValue = new TypedValue(null, DataType.OBJECT_DATA_TYPE);
        when(mockExpressionManager.evaluateTyped(EXPRESSION, mockEvent)).thenReturn(typedValue);
        addVariableTransformer.initialise();

        addVariableTransformer.transform(mockEvent, ENCODING);

        verifyRemoved(mockEvent, PLAIN_STRING_KEY);
    }

    protected abstract void verifyAdded(MuleEvent event, String key, String value, ArgumentCaptor<DataType> dataTypeCaptor);

    protected abstract void verifyNotAdded(MuleEvent mockEvent);

    protected abstract void verifyRemoved(MuleEvent mockEvent, String key);

}
