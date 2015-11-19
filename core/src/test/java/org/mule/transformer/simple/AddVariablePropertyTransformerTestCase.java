/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.transformer.types.MimeTypes.APPLICATION_XML;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.matcher.DataTypeMatcher;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.MimeTypes;
import org.mule.transformer.types.TypedValue;

import java.util.Arrays;
import java.util.Collection;

import javax.activation.MimeTypeParseException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(Parameterized.class)
@SmallTest
public class AddVariablePropertyTransformerTestCase extends AbstractMuleTestCase
{
    public static final String ENCODING = "encoding";
    public static final String PLAIN_STRING_KEY = "someText";
    public static final String PLAIN_STRING_VALUE = "someValue";
    public static final String EXPRESSION = "#[string:someValue]";
    public static final String EXPRESSION_VALUE = "expressionValueResult";
    public static final String NULL_EXPRESSION = "#[string:someValueNull]";
    public static final String CUSTOM_ENCODING = UTF_8.name();

    private MuleMessage mockMessage = Mockito.mock(MuleMessage.class);
    private MuleContext mockMuleContext = Mockito.mock(MuleContext.class);
    private ExpressionManager mockExpressionManager = Mockito.mock(ExpressionManager.class);
    private AbstractAddVariablePropertyTransformer addVariableTransformer;
    private PropertyScope scope;
    private final ArgumentCaptor<DataType> dataTypeCaptor = ArgumentCaptor.forClass(DataType.class);

    public AddVariablePropertyTransformerTestCase(AbstractAddVariablePropertyTransformer abstractAddVariableTransformer,
                                                  PropertyScope scope)
    {
        this.addVariableTransformer = abstractAddVariableTransformer;
        this.scope = scope;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{new AddFlowVariableTransformer(), PropertyScope.INVOCATION},
            {new AddSessionVariableTransformer(), PropertyScope.SESSION},
            {new AddPropertyTransformer(), PropertyScope.OUTBOUND}});
    }

    @Before
    public void setUpTest() throws MimeTypeParseException
    {
        addVariableTransformer.setEncoding(null);
        addVariableTransformer.setMimeType(null);

        when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
        when(mockExpressionManager.parse(anyString(), Mockito.any(MuleMessage.class))).thenAnswer(
            new Answer<String>()
            {
                @Override
                public String answer(InvocationOnMock invocation) throws Throwable
                {

                    return (String) invocation.getArguments()[0];
                }
            });
        when(mockExpressionManager.evaluate(EXPRESSION, mockMessage)).thenReturn(EXPRESSION_VALUE);
        TypedValue typedValue = new TypedValue(EXPRESSION_VALUE, DataTypeFactory.STRING);
        when(mockExpressionManager.evaluateTyped(EXPRESSION, mockMessage)).thenReturn(typedValue);
        addVariableTransformer.setMuleContext(mockMuleContext);
    }

    @Test
    public void testAddVariable() throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        addVariableTransformer.setValue(PLAIN_STRING_VALUE);
        addVariableTransformer.initialise();
        addVariableTransformer.transform(mockMessage, ENCODING);

        verify(mockMessage).setProperty(argThat(equalTo(PLAIN_STRING_KEY)), argThat(equalTo(PLAIN_STRING_VALUE)), argThat(equalTo(scope)), dataTypeCaptor.capture());
        assertThat(dataTypeCaptor.getValue(), DataTypeMatcher.like(String.class, MimeTypes.ANY, null));
    }

    @Test
    public void testAddVariableWithExpressionValue() throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        addVariableTransformer.setValue(EXPRESSION);
        addVariableTransformer.initialise();
        addVariableTransformer.transform(mockMessage, ENCODING);

        verify(mockMessage).setProperty(argThat(equalTo(PLAIN_STRING_KEY)), argThat(equalTo(EXPRESSION_VALUE)), argThat(equalTo(scope)), dataTypeCaptor.capture());
        assertThat(dataTypeCaptor.getValue(), DataTypeMatcher.like(String.class, MimeTypes.ANY, null));
    }

    @Test
    public void testAddVariableWithExpressionKey() throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(EXPRESSION);
        addVariableTransformer.setValue(PLAIN_STRING_VALUE);
        addVariableTransformer.initialise();
        addVariableTransformer.transform(mockMessage, ENCODING);

        verify(mockMessage).setProperty(argThat(equalTo(EXPRESSION_VALUE)), argThat(equalTo(PLAIN_STRING_VALUE)), argThat(equalTo(scope)), dataTypeCaptor.capture());
        assertThat(dataTypeCaptor.getValue(), DataTypeMatcher.like(String.class, MimeTypes.ANY, null));
    }

    @Test
    public void testAddVariableWithEncoding() throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        addVariableTransformer.setValue(PLAIN_STRING_VALUE);
        addVariableTransformer.initialise();
        addVariableTransformer.setEncoding(CUSTOM_ENCODING);
        addVariableTransformer.transform(mockMessage, ENCODING);

        verify(mockMessage).setProperty(argThat(equalTo(PLAIN_STRING_KEY)), argThat(equalTo(PLAIN_STRING_VALUE)), argThat(equalTo(scope)), dataTypeCaptor.capture());
        assertThat(dataTypeCaptor.getValue(), DataTypeMatcher.like(String.class, MimeTypes.ANY, CUSTOM_ENCODING));
    }

    @Test
    public void testAddVariableWithMimeType() throws InitialisationException, TransformerException, MimeTypeParseException
    {
        addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        addVariableTransformer.setValue(PLAIN_STRING_VALUE);
        addVariableTransformer.initialise();
        addVariableTransformer.setMimeType(APPLICATION_XML);
        addVariableTransformer.transform(mockMessage, ENCODING);

        verify(mockMessage).setProperty(argThat(equalTo(PLAIN_STRING_KEY)), argThat(equalTo(PLAIN_STRING_VALUE)), argThat(equalTo(scope)), dataTypeCaptor.capture());
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
        addVariableTransformer.transform(mockMessage, ENCODING);
        verify(mockMessage, VerificationModeFactory.times(0)).setProperty((String) isNull(), anyString(),
            Matchers.<PropertyScope> anyObject());
    }

    @Test
    public void testAddVariableWithNullExpressionValueResult()
        throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        TypedValue typedValue = new TypedValue(null, DataType.OBJECT_DATA_TYPE);
        when(mockExpressionManager.evaluateTyped(NULL_EXPRESSION, mockMessage)).thenReturn(typedValue);
        addVariableTransformer.setValue(NULL_EXPRESSION);
        addVariableTransformer.initialise();
        addVariableTransformer.transform(mockMessage, ENCODING);
        verify(mockMessage, VerificationModeFactory.times(1)).removeProperty(PLAIN_STRING_KEY, scope);
    }

    @Test
    public void testAddVariableWithNullPayloadExpressionValueResult()
            throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        addVariableTransformer.setValue(EXPRESSION);
        TypedValue typedValue = new TypedValue(null, DataType.OBJECT_DATA_TYPE);
        when(mockExpressionManager.evaluateTyped(EXPRESSION, mockMessage)).thenReturn(typedValue);
        addVariableTransformer.initialise();

        addVariableTransformer.transform(mockMessage, ENCODING);

        verify(mockMessage, VerificationModeFactory.times(1)).removeProperty(PLAIN_STRING_KEY, scope);
    }
}
