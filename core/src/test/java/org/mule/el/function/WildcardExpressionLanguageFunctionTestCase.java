/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.function;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.el.ExpressionExecutor;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.el.context.MessageContext;
import org.mule.el.mvel.MVELExpressionExecutor;
import org.mule.el.mvel.MVELExpressionLanguageContext;
import org.mule.mvel2.CompileException;
import org.mule.mvel2.ParserConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class WildcardExpressionLanguageFunctionTestCase extends AbstractMuleTestCase
{

    protected ExpressionExecutor<MVELExpressionLanguageContext> expressionExecutor;
    protected MVELExpressionLanguageContext context;
    protected WildcardExpressionLanguageFuntion wildcardFunction;

    @Before
    public void setup() throws InitialisationException
    {
        ParserConfiguration parserConfiguration = new ParserConfiguration();
        expressionExecutor = new MVELExpressionExecutor(parserConfiguration);
        context = new MVELExpressionLanguageContext(parserConfiguration, Mockito.mock(MuleContext.class));
        wildcardFunction = new WildcardExpressionLanguageFuntion();
        context.declareFunction("wildcard", wildcardFunction);
    }

    @Test
    public void testReturnFalseWhenDoesNotMatches() throws Exception
    {
        addMessageToContextWithPayload("TEST");
        boolean result = (Boolean) wildcardFunction.call(new Object[]{"'*ASDF*QWER*'"}, context);
        assertFalse(result);
    }

    @Test
    public void testReturnFalseWhenDoesNotMatchesMVEL() throws Exception
    {
        addMessageToContextWithPayload("TEST");
        boolean result = (Boolean) expressionExecutor.execute("wildcard('*ASDF*QWER*')", context);
        assertFalse(result);
    }

    @Test
    public void testReturnsTrueWhenMatches() throws Exception
    {
        addMessageToContextWithPayload("TESTfooTEST");
        boolean result = (Boolean) wildcardFunction.call(new Object[]{"TEST*TEST"}, context);
        assertTrue(result);
    }

    @Test
    public void testReturnsTrueWhenMatchesMVEL() throws Exception
    {
        addMessageToContextWithPayload("TESTfooTEST");
        boolean result = (Boolean) expressionExecutor.execute("wildcard('TEST*TEST')", context);
        assertTrue(result);
    }

    @Test
    public void testReturnFalseWhenDoesNotMatchesDefinedTextArgument() throws Exception
    {
        boolean result = (Boolean) wildcardFunction.call(new Object[]{"'*ASDF*QWER*'", "TEST"}, context);
        assertFalse(result);
    }

    @Test
    public void testReturnFalseWhenDoesNotMatchesDefinedTextArgumentMVEL() throws Exception
    {
        boolean result = (Boolean) expressionExecutor.execute("wildcard('*ASDF*QWER*', 'TEST')", context);
        assertFalse(result);
    }

    @Test
    public void testReturnsTrueWhenMatchesDefinedTextArgument() throws Exception
    {
        boolean result = (Boolean) wildcardFunction.call(new Object[]{"TEST*TEST", "TESTfooTEST"}, context);
        assertTrue(result);
    }

    @Test
    public void testReturnsTrueWhenMatchesDefinedTextArgumentMVEL() throws Exception
    {
        addMessageToContextWithPayload("TESTfooTEST");
        boolean result = (Boolean) expressionExecutor.execute("wildcard('TEST*TEST', 'TESTfooTEST')", context);
        assertTrue(result);
    }

    @Test
    public void testReturnFalseWhenDoesNotMatchesDefinedTextArgumentAndSensitivityIsTrue() throws Exception
    {
        boolean result = (Boolean) wildcardFunction.call(new Object[]{"'tes*'", "TEST", true}, context);
        assertFalse(result);
    }

    @Test
    public void testReturnFalseWhenDoesNotMatchesDefinedTextArgumentAndSensitivityMVELIsTrue() throws Exception
    {
        boolean result = (Boolean) expressionExecutor.execute("wildcard('tes?', 'TEST', true)", context);
        assertFalse(result);
    }

    @Test
    public void testReturnFalseWhenDoesNotMatchesDefinedTextArgumentAndSensitivityIsFalse() throws Exception
    {
        boolean result = (Boolean) wildcardFunction.call(new Object[]{"'*ASDF*QWER*'", "TEST", false}, context);
        assertFalse(result);
    }

    @Test
    public void testReturnFalseWhenDoesNotMatchesDefinedTextArgumentAndSensitivityMVELIsFalse() throws Exception
    {
        boolean result = (Boolean) expressionExecutor.execute("wildcard('*ASDF*QWER*', 'TEST', false)", context);
        assertFalse(result);
    }

    @Test
    public void testReturnsTrueWhenMatchesDefinedTextArgumentAndSensitivityIsTrue() throws Exception
    {
        boolean result = (Boolean) wildcardFunction.call(new Object[]{"test*TEST", "testfooTEST", true}, context);
        assertTrue(result);
    }

    @Test
    public void testReturnsTrueWhenMatchesDefinedTextArgumentAndSensitivityMVELIsTrue() throws Exception
    {
        addMessageToContextWithPayload("TESTfooTEST");
        boolean result = (Boolean) expressionExecutor.execute("wildcard('test*TEST', 'testfooTEST', true)", context);
        assertTrue(result);
    }

    @Test
    public void testReturnsTrueWhenMatchesDefinedTextArgumentAndSensitivityIsFalse() throws Exception
    {
        boolean result = (Boolean) wildcardFunction.call(new Object[]{"TEST*test", "testfooTEST", false}, context);
        assertTrue(result);
    }

    @Test
    public void testReturnsTrueWhenMatchesDefinedTextArgumentAndSensitivityMVELIsFalse() throws Exception
    {
        addMessageToContextWithPayload("TESTfooTEST");
        boolean result = (Boolean) expressionExecutor.execute("wildcard('TEST???test', 'testfooTEST', false)", context);
        assertTrue(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidNullPattern() throws Exception
    {
        wildcardFunction.call(new Object[]{null}, context);
    }

    @Test(expected = CompileException.class)
    public void testInvalidNullPatternMVEL() throws Exception
    {
        expressionExecutor.execute("wildcard(null)", context);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidNonStringPattern() throws Exception
    {
        wildcardFunction.call(new Object[]{new Date()}, context);
    }

    @Test(expected = CompileException.class)
    public void testInvalidNonStringWildcardPatternMVEL() throws Exception
    {
        expressionExecutor.execute("wildcard(new java.util.Date())", context);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidNullText() throws Exception
    {
        wildcardFunction.call(new Object[]{"TEST*TEST", null}, context);
    }

    @Test(expected = CompileException.class)
    public void testInvalidNullTextMVEL() throws Exception
    {
        expressionExecutor.execute("wildcard('TEST*TEST',null)", context);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidNonStringText() throws Exception
    {
        wildcardFunction.call(new Object[]{"TEST*TEST", new Date()}, context);
    }

    @Test(expected = CompileException.class)
    public void testInvalidNonStringTextMVEL() throws Exception
    {
        expressionExecutor.execute("wildcard('TEST*TEST',new Date())", context);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidBlankStringText() throws Exception
    {
        wildcardFunction.call(new Object[]{"", "testfooTEST"}, context);
    }

    @Test(expected = CompileException.class)
    public void testInvalidBlankStringTextMVEL() throws Exception
    {
        expressionExecutor.execute("wildcard('', 'testfooTEST')", context);
    }

    @SuppressWarnings("unchecked")
    protected void addMessageToContextWithPayload(String payload) throws TransformerException
    {
        MuleMessage message = Mockito.mock(MuleMessage.class);
        Mockito.when(message.getPayload(Mockito.any(Class.class))).thenReturn(payload);
        context.addFinalVariable("message", new MessageContext(message));
    }

}
