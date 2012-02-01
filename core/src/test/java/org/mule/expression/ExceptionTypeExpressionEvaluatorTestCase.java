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

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExceptionTypeExpressionEvaluatorTestCase extends AbstractMuleTestCase
{

    private ExceptionTypeExpressionEvaluator exceptionTypeExpressionEvaluator = new ExceptionTypeExpressionEvaluator();
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleMessage mockMuleMessage;

    @Test
    public void testEvaluateNotMatchingExpression() throws Exception
    {
        Assert.assertThat((Boolean) exceptionTypeExpressionEvaluator.evaluate("asdf", mockMuleMessage), Is.is(false));
    }

    @Test
    public void testEvaluateMatchingExceptionType() throws Exception
    {
        configureExceptionPayload(new NullPointerException());
        Assert.assertThat((Boolean) exceptionTypeExpressionEvaluator.evaluate(NullPointerException.class.getName(), mockMuleMessage), Is.is(true));
    }
    
    @Test
    public void testEvaluateMatchingSubtype() throws Exception
    {
        configureExceptionPayload(new NullPointerException());
        Assert.assertThat((Boolean)exceptionTypeExpressionEvaluator.evaluate(RuntimeException.class.getName(), mockMuleMessage),Is.is(true));
    }
    
    @Test
    public void testEvaluateExactMatchingType() throws Exception
    {
        configureExceptionPayload(new NullPointerException());
        Assert.assertThat((Boolean)exceptionTypeExpressionEvaluator.evaluate("=" + NullPointerException.class.getName(), mockMuleMessage),Is.is(true));
    }
    
    @Test
    public void testEvaluateExactSubtype() throws Exception
    {
        configureExceptionPayload(new NullPointerException());
        Assert.assertThat((Boolean)exceptionTypeExpressionEvaluator.evaluate("=" + RuntimeException.class.getName(), mockMuleMessage),Is.is(false));
    }
    
    @Test
    public void testEvaluateMatchingUsingRegex() throws Exception
    {
        configureExceptionPayload(new MuleRuntimeException(new Exception()));
        Assert.assertThat((Boolean)exceptionTypeExpressionEvaluator.evaluate("(.*).mule.(.*)Exception", mockMuleMessage),Is.is(true));
    }
    @Test
    public void testEvaluateNotMatchingUsingRegex() throws Exception
    {
        configureExceptionPayload(new NullPointerException());
        Assert.assertThat((Boolean)exceptionTypeExpressionEvaluator.evaluate("(.*).mule.not.matching.(.*)Exception", mockMuleMessage),Is.is(false));
    }

    private void configureExceptionPayload(Exception e)
    {
        Mockito.when(mockMuleMessage.getExceptionPayload().getException()).thenReturn(e);
    }
}
