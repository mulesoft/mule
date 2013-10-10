/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.el.mvel;

import static org.junit.Assert.assertEquals;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mvel2.CompileException;
import org.mvel2.ParserContext;

@SmallTest
public class MVELExpressionExecutorTestCase extends AbstractMuleTestCase
{
    protected MVELExpressionExecutor mvel;
    protected MVELExpressionLanguageContext context;

    @Before
    public void setupMVEL() throws InitialisationException
    {
        mvel = new MVELExpressionExecutor(new ParserContext());
        context = Mockito.mock(MVELExpressionLanguageContext.class);
        Mockito.when(context.isResolveable(Mockito.anyString())).thenReturn(false);
    }

    @Test
    public void evaluateReturnInt()
    {
        assertEquals(4, mvel.execute("2*2", null));
    }

    @Test
    public void evaluateReturnString()
    {
        assertEquals("hi", mvel.execute("'hi'", null));
    }

    @Test(expected = CompileException.class)
    public void evaluateInvalidExpression()
    {
        assertEquals(4, mvel.execute("2*'2", null));
    }

    @Test(expected = CompileException.class)
    public void invalidExpression()
    {
        mvel.validate("a9-#'");
    }

    @Test
    public void validExpression()
    {
        mvel.validate("var a = 2");
    }

}
