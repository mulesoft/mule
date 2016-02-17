/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.metadata.SimpleDataType;
import org.mule.api.transformer.TransformerException;
import org.mule.PropertyScope;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;

@RunWith(Parameterized.class)
@SmallTest
public class RemoveVariablePropertyTransformerTest extends AbstractMuleTestCase
{
    public static final String ENCODING = "encoding";
    public static final String PLAIN_STRING_KEY = "someText";
    public static final String PLAIN_STRING_VALUE = "someValue";
    public static final String EXPRESSION = "#[string:someValue]";
    public static final String EXPRESSION_VALUE = "expressionValueResult";
    public static final String NULL_EXPRESSION = "#[string:someValueNull]";
    public static final String NULL_EXPRESSION_VALUE = null;

    private MuleEvent mockEvent = mock(MuleEvent.class);
    private MuleMessage mockMessage = Mockito.mock(MuleMessage.class, RETURNS_DEEP_STUBS);
    private MuleContext mockMuleContext = Mockito.mock(MuleContext.class);
    private ExpressionManager mockExpressionManager = Mockito.mock(ExpressionManager.class);
    private AbstractRemoveVariablePropertyTransformer removeVariableTransformer;
    private PropertyScope scope;

    public RemoveVariablePropertyTransformerTest(AbstractRemoveVariablePropertyTransformer abstractAddVariableTransformer, PropertyScope scope)
    {
        this.removeVariableTransformer = abstractAddVariableTransformer;
        this.scope = scope;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
                {new RemoveFlowVariableTransformer(), PropertyScope.INVOCATION},
                {new RemoveSessionVariableTransformer(), PropertyScope.SESSION},
                {new RemovePropertyTransformer(), PropertyScope.OUTBOUND}
        });
    }

    @Before
    public void setUpTest()
    {
        when(mockEvent.getMessage()).thenReturn(mockMessage);
        when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
        when(mockExpressionManager.parse(anyString(), Mockito.any(MuleEvent.class))).thenAnswer(
                invocation -> invocation.getArguments()[0]);
        when(mockExpressionManager.evaluate(EXPRESSION, mockEvent)).thenReturn(EXPRESSION_VALUE);
        removeVariableTransformer.setMuleContext(mockMuleContext);
        when(mockMessage.getDataType()).thenReturn(new SimpleDataType(String.class));
    }

    @Test
    public void testRemoveVariable() throws InitialisationException, TransformerException
    {
        removeVariableTransformer.setIdentifier(PLAIN_STRING_KEY);
        removeVariableTransformer.initialise();
        removeVariableTransformer.transform(mockMessage, ENCODING);
        verify(mockMessage).removeProperty(PLAIN_STRING_KEY, scope);
    }

    @Test
    public void testRemoveVariableUsingExpression() throws InitialisationException, TransformerException
    {
        removeVariableTransformer.setIdentifier(EXPRESSION);
        removeVariableTransformer.initialise();
        removeVariableTransformer.transform(mockEvent, ENCODING);
        verify(mockMessage).removeProperty(EXPRESSION_VALUE, scope);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveVariableNullKey() throws InitialisationException, TransformerException
    {
        removeVariableTransformer.setIdentifier(null);
    }

    @Test //Don't fail.
    public void testRemoveVariableExpressionKeyNullValue() throws InitialisationException, TransformerException
    {
        removeVariableTransformer.setIdentifier(NULL_EXPRESSION);
        removeVariableTransformer.initialise();
        removeVariableTransformer.transform(mockMessage, ENCODING);
    }

    @Test
    @Ignore
    public void testRemoveVariableWithRegexExpression() throws InitialisationException, TransformerException
    {
        Mockito.when(mockMessage.getPropertyNames(scope)).thenReturn(new HashSet<String>(Arrays.asList("MULE_ID","MULE_CORRELATION_ID","SomeVar","MULE_GROUP_ID")));
        removeVariableTransformer.setIdentifier("MULE_(.*)");
        removeVariableTransformer.initialise();
        removeVariableTransformer.transform(mockMessage, ENCODING);
        verify(mockMessage).removeProperty("MULE_ID", scope);
        verify(mockMessage).removeProperty("MULE_CORRELATION_ID", scope);
        verify(mockMessage).removeProperty("MULE_GROUP_ID", scope);
        verify(mockMessage, VerificationModeFactory.times(0)).removeProperty("SomeVar", scope);
    }
}
