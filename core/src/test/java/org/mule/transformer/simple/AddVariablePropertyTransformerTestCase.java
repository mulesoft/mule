/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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
    public static final String NULL_EXPRESSION_VALUE = null;

    private MuleMessage mockMessage = Mockito.mock(MuleMessage.class);
    private MuleContext mockMuleContext = Mockito.mock(MuleContext.class);
    private ExpressionManager mockExpressionManager = Mockito.mock(ExpressionManager.class);
    private AbstractAddVariablePropertyTransformer addVariableTransformer;
    private PropertyScope scope;

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
    public void setUpTest()
    {
        Mockito.when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
        Mockito.when(mockExpressionManager.parse(anyString(), Mockito.any(MuleMessage.class))).thenAnswer(
            new Answer<String>()
            {
                @Override
                public String answer(InvocationOnMock invocation) throws Throwable
                {

                    return (String) invocation.getArguments()[0];
                }
            });
        Mockito.when(mockExpressionManager.evaluate(EXPRESSION, mockMessage)).thenReturn(EXPRESSION_VALUE);
        addVariableTransformer.setMuleContext(mockMuleContext);
    }

    @Test
    public void testAddVariable() throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        addVariableTransformer.setValue(PLAIN_STRING_VALUE);
        addVariableTransformer.initialise();
        addVariableTransformer.transform(mockMessage, ENCODING);
        verify(mockMessage).setProperty(PLAIN_STRING_KEY, PLAIN_STRING_VALUE, scope);
    }

    @Test
    public void testAddVariableWithExpressionValue() throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        addVariableTransformer.setValue(EXPRESSION);
        addVariableTransformer.initialise();
        addVariableTransformer.transform(mockMessage, ENCODING);
        verify(mockMessage).setProperty(PLAIN_STRING_KEY, EXPRESSION_VALUE, scope);
    }

    @Test
    public void testAddVariableWithExpressionKey() throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(EXPRESSION);
        addVariableTransformer.setValue(PLAIN_STRING_VALUE);
        addVariableTransformer.initialise();
        addVariableTransformer.transform(mockMessage, ENCODING);
        verify(mockMessage).setProperty(EXPRESSION_VALUE, PLAIN_STRING_VALUE, scope);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddVariableWithNullKey() throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(null);
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
        Mockito.verify(mockMessage, VerificationModeFactory.times(0)).setProperty(anyString(), anyString(),
            Matchers.<PropertyScope> anyObject());
    }

    @Test
    public void testAddVariableWithNullExpressionValueResult()
        throws InitialisationException, TransformerException
    {
        addVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        addVariableTransformer.setValue(NULL_EXPRESSION);
        addVariableTransformer.initialise();
        addVariableTransformer.transform(mockMessage, ENCODING);
        Mockito.verify(mockMessage, VerificationModeFactory.times(0)).setProperty(anyString(), anyString(),
            Matchers.<PropertyScope> anyObject());
    }

}
