/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleContext;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.lifecycle.InitialisationException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MVELExpressionLanguageTestCase
{

    protected MVELExpressionLanguage mvel;

    @Before
    public void setupMVEL() throws InitialisationException
    {
        MuleContext muleContext = Mockito.mock(MuleContext.class);
        mvel = new MVELExpressionLanguage(muleContext);
        mvel.initialise();
    }

    @Test
    public void evaluateReturnInt()
    {
        assertEquals(4, mvel.evaluate("2*2"));
    }

    @Test
    public void evaluateReturnString()
    {
        assertEquals("hi", mvel.evaluate("'hi'"));
    }

    @Test(expected = ExpressionRuntimeException.class)
    public void evaluateInvalidExpression()
    {
        assertEquals(4, mvel.evaluate("2*'2"));
    }

    @Test
    public void evaluateWithIntVar()
    {
        assertEquals(4, mvel.evaluate("a*2", Collections.singletonMap("a", 2)));
    }

    @Test
    public void evaluateWithStringVar()
    {
        assertEquals("Hi Dan", mvel.evaluate("'Hi '#a", Collections.singletonMap("a", "Dan")));
    }

    @Test
    public void evaluateWithMultipleVars()
    {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("a", "Dan");
        vars.put("b", 2);
        assertEquals("Hi Dan2", mvel.evaluate("'Hi '#a#b", vars));
    }

    @Test(expected = ExpressionRuntimeException.class)
    public void evaluateInvalidExpressionWithVars()
    {
        assertEquals(4, mvel.evaluate("2*'2", Collections.singletonMap("a", 2)));
    }

    @Test
    public void invalidExpression()
    {
        assertFalse(mvel.isValid("a9-#'"));
    }

    @Test
    public void validExpression()
    {
        assertTrue(mvel.isValid("var a = 2"));
    }

}
