/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.NullPayload;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SmallTest
public class SetPayloadTransformerTestCase extends AbstractMuleTestCase
{
    private static final String PLAIN_TEXT = "This is a plain text";
    private static final String EXPRESSION = "#[testVariable]";

    private SetPayloadTransformer setPayloadTransformer;
    private MuleContext mockMuleContext;
    private MuleMessage mockMuleMessage;
    private ExpressionManager mockExpressionManager;

    @Before
    public void setUp()
    {
        setPayloadTransformer = new SetPayloadTransformer();
        mockMuleContext = mock(MuleContext.class);
        setPayloadTransformer.setMuleContext(mockMuleContext);
        mockExpressionManager = mock(ExpressionManager.class);
        mockMuleMessage = mock(MuleMessage.class);

        when(mockMuleContext.getExpressionManager()).thenReturn(mockExpressionManager);
        Mockito.when(mockExpressionManager.parse(anyString(), Mockito.any(MuleMessage.class))).thenAnswer(
            new Answer<String>()
            {
                @Override
                public String answer(InvocationOnMock invocation) throws Throwable
                {

                    return (String) invocation.getArguments()[0];
                }
            });
    }

    @Test
    public void testSetPayloadTransformerNulValue() throws InitialisationException, TransformerException
    {
        setPayloadTransformer.setValue(null);
        setPayloadTransformer.initialise();

        Object response = setPayloadTransformer.transformMessage(mockMuleMessage, "UTF-8");
        assertTrue(response instanceof NullPayload);
    }

    @Test
    public void testSetPayloadTransformerPlainText() throws InitialisationException, TransformerException
    {
        setPayloadTransformer.setValue(PLAIN_TEXT);
        setPayloadTransformer.initialise();

        when(mockExpressionManager.isExpression(PLAIN_TEXT)).thenReturn(false);

        Object response = setPayloadTransformer.transformMessage(mockMuleMessage, "UTF-8");
        assertEquals(PLAIN_TEXT, response);
    }

    @Test
    public void testSetPayloadTransformerExpression() throws InitialisationException, TransformerException
    {
        setPayloadTransformer.setValue(EXPRESSION);
        when(mockExpressionManager.isExpression(EXPRESSION)).thenReturn(true);
        setPayloadTransformer.initialise();
        when(mockExpressionManager.evaluate(EXPRESSION, mockMuleMessage)).thenReturn(PLAIN_TEXT);

        Object response = setPayloadTransformer.transformMessage(mockMuleMessage, "UTF-8");
        assertEquals(PLAIN_TEXT, response);
    }

}
