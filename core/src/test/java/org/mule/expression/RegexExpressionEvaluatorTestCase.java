/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class RegexExpressionEvaluatorTestCase extends AbstractMuleTestCase
{

    private RegexExpressionEvaluator regexExpressionEvaluator;
    private MuleContext muleContext;

    @Before
    public void setUp() throws Exception
    {
        MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
        when(muleConfiguration.isCacheMessageAsBytes()).thenReturn(false);
        muleContext = Mockito.mock(MuleContext.class);
        when(muleContext.getConfiguration()).thenReturn(muleConfiguration);

        regexExpressionEvaluator = new RegexExpressionEvaluator();
    }

    @Test
    public void testReturnNullWhenDoesNotMatches() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("TEST", muleContext);

        Object result = regexExpressionEvaluator.evaluate("TESTw+TEST", message);
        assertNull(result);
    }

    @Test
    public void testReturnsPayloadWhenMatchesIfNoCaptureGroupDefined() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("TESTfooTEST", muleContext);

        Object result = regexExpressionEvaluator.evaluate("TEST\\w+TEST", message);
        assertEquals("TESTfooTEST", result);
    }

    @Test
    public void testReturnsMatchedValueIfCaptureGroupDefined() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("TESTfooTEST", muleContext);

        Object result = regexExpressionEvaluator.evaluate("TEST(\\w+)TEST", message);
        assertEquals("foo", result);
    }

    @Test
    public void testReturnsMultipleValuesIfMultipleCaptureGroupDefine() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("TESTfooTESTbar", muleContext);

        Object result = regexExpressionEvaluator.evaluate("TEST(\\w+)TEST(\\w+)", message);

        assertTrue(result instanceof String[]);
        String[] values = (String[]) result;
        assertEquals(2, values.length);
        assertEquals("foo", values[0]);
        assertEquals("bar", values[1]);
    }
}
