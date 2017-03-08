/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


package org.mule.routing;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.expression.ExpressionManager;
import org.mule.expression.ExpressionConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;


public class ExpressionSplitterIteratorTestCase extends AbstractMuleTestCase
{

    private MuleEvent muleEvent = mock(MuleEvent.class);
    private MuleContext muleContext = mock(MuleContext.class);
    private ExpressionManager expressionManager = mock(ExpressionManager.class);
    private final List<Integer> integers = createListOfIntegers();
    private final ExpressionConfig expressionConfig = mock(ExpressionConfig.class);
    private final ExpressionSplitter expressionSplitter = new ExpressionSplitter(expressionConfig);
    private final MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);

    @Before
    public void setUp() throws Exception
    {
        expressionSplitter.setMuleContext(muleContext);
        when(muleEvent.getMuleContext()).thenReturn(muleContext);
        when(muleContext.getExpressionManager()).thenReturn(expressionManager);
        when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
        when(expressionConfig.getFullExpression(any(ExpressionManager.class))).thenReturn("fullExpression");
        when(expressionManager.evaluate(any(String.class), any(MuleEvent.class))).thenReturn(integers.iterator());
    }

    @Test
    public void testExpressionSplitterWithIteratorInput() throws Exception
    {
        List<MuleMessage> muleMessages = expressionSplitter.splitMessage(muleEvent);
        assertThat(muleMessages.size(), is(integers.size()));
        assertListValues(muleMessages);
    }

    private List<Integer> createListOfIntegers()
    {
        List<Integer> integers = new ArrayList<>(3);
        for (int i = 0; i < 3; i++)
        {
            integers.add(i);
        }
        return integers;
    }

    private void assertListValues(List<MuleMessage> messages)
    {
        Integer i = 0;
        for (MuleMessage message : messages)
        {
            assertThat(message.getPayload(), instanceOf(Integer.class));
            assertThat(message.getPayload(), is((Object) i++));
        }
    }

}
