/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.function;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.el.ExpressionExecutor;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.el.context.MessageContext;
import org.mule.el.mvel.MVELExpressionExecutor;
import org.mule.el.mvel.MVELExpressionLanguageContext;
import org.mule.mvel2.CompileException;
import org.mule.mvel2.ParserContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

@SmallTest
public class WilcardExpressionLanguageFunctionTestCase extends AbstractMuleTestCase
{

    protected ExpressionExecutor<MVELExpressionLanguageContext> expressionExecutor;
    protected MVELExpressionLanguageContext context;
    protected WildcardExpressionLanguageFuntion wildcardFuntion;

    @Before
    public void setup() throws InitialisationException
    {
        ParserContext parserContext = new ParserContext();
        expressionExecutor = new MVELExpressionExecutor(parserContext);
        context = new MVELExpressionLanguageContext(parserContext, Mockito.mock(MuleContext.class));
        wildcardFuntion = new WildcardExpressionLanguageFuntion();
        context.declareFunction("wildcard", wildcardFuntion);
    }

    @Test
    public void testReturnFalseWhenDoesNotMatches() throws Exception
    {
        addMessageToContextWithPayload("TEST");
        boolean result = (Boolean) wildcardFuntion.call(new Object[]{"'*ASDF*QWER*'"}, context);
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
        boolean result = (Boolean) wildcardFuntion.call(new Object[]{"TEST*TEST"}, context);
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
        boolean result = (Boolean) wildcardFuntion.call(new Object[]{"'*ASDF*QWER*'", "TEST"}, context);
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
        boolean result = (Boolean) wildcardFuntion.call(new Object[]{"TEST*TEST", "TESTfooTEST"}, context);
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
    public void testReturnFalseWhenDoesNotMatchesDefinedTextArgumentAndSensitivity() throws Exception
    {
        boolean result = (Boolean) wildcardFuntion.call(new Object[]{"'*ASDF*QWER*'", "TEST", true}, context);
        assertFalse(result);
    }

    @Test
    public void testReturnFalseWhenDoesNotMatchesDefinedTextArgumentAndSensitivityMVEL() throws Exception
    {
        boolean result = (Boolean) expressionExecutor.execute("wildcard('*ASDF*QWER*', 'TEST', true)", context);
        assertFalse(result);
    }

    @Test
    public void testReturnsTrueWhenMatchesDefinedTextArgumentAndSensitivity() throws Exception
    {
        boolean result = (Boolean) wildcardFuntion.call(new Object[]{"test*TEST", "testfooTEST", true}, context);
        assertTrue(result);
    }

    @Test
    public void testReturnsTrueWhenMatchesDefinedTextArgumentAndSensitivityMVEL() throws Exception
    {
        addMessageToContextWithPayload("TESTfooTEST");
        boolean result = (Boolean) expressionExecutor.execute("wildcard('test*TEST', 'testfooTEST', true)", context);
        assertTrue(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidNullPattern() throws Exception
    {
        wildcardFuntion.call(new Object[]{null}, context);
    }

    @Test(expected = CompileException.class)
    public void testInvalidNullPatternMVEL() throws Exception
    {
        expressionExecutor.execute("wildcard(null)", context);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidNonStringPattern() throws Exception
    {
        wildcardFuntion.call(new Object[]{new Date()}, context);
    }

    @Test(expected = CompileException.class)
    public void testInvalidNonStringWildcardPatternMVEL() throws Exception
    {
        expressionExecutor.execute("wildcard(new java.util.Date())", context);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidNullText() throws Exception
    {
        wildcardFuntion.call(new Object[]{"TEST*TEST", null}, context);
    }

    @Test(expected = CompileException.class)
    public void testInvalidNullTextMVEL() throws Exception
    {
        expressionExecutor.execute("wildcard('TEST*TEST',null)", context);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidNonStringText() throws Exception
    {
        wildcardFuntion.call(new Object[]{"TEST*TEST", new Date()}, context);
    }

    @Test(expected = CompileException.class)
    public void testInvalidNonStringTextMVEL() throws Exception
    {
        expressionExecutor.execute("wildcard('TEST*TEST',new Date())", context);
    }

    @SuppressWarnings("unchecked")
	protected void addMessageToContextWithPayload(String payload) throws TransformerException
    {
        MuleMessage message = Mockito.mock(MuleMessage.class);
        Mockito.when(message.getPayload(Mockito.any(Class.class))).thenReturn(payload);
        context.addFinalVariable("message", new MessageContext(message));
    }

}
