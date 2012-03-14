/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.el;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.el.ExpressionLanguage;
import org.mule.api.expression.ExpressionManager;
import org.mule.el.mvel.MVELExpressionLanguage;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

public class ExpressionLanguageConfigTestCase extends FunctionalTestCase
{

    ExpressionLanguage el;
    ExpressionManager em;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/el/expression-language-config.xml";
    }

    @Before
    public void setup()
    {
        el = muleContext.getExpressionLanguage();
        em = muleContext.getExpressionManager();
    }

    @Test
    public void testExpressionLanguage()
    {
        assertNotNull(el);
        assertEquals(MVELExpressionLanguage.class, el.getClass());
    }

    @Test
    public void testExpressionLanguageAlias()
    {
        assertEquals(muleContext.getConfiguration().getId(), el.evaluate("appName"));
        assertEquals(muleContext.getConfiguration().getId(), em.evaluate("appName", (MuleMessage) null));
    }

    @Test
    public void testExpressionLanguageGlobalFunction()
    {
        assertEquals("Hello " + muleContext.getConfiguration().getId() + "!", el.evaluate("hello()"));
        assertEquals("Hello " + muleContext.getConfiguration().getId() + "!",
            em.evaluate("hello()", (MuleMessage) null));
    }

    @Test
    public void testExpressionLanguageExecuteElement() throws Exception
    {
        testFlow("flow", getTestEvent("foo"));
    }

}
