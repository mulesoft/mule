/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.el;

import static org.junit.Assert.assertEquals;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.runtime.core.api.expression.ExpressionManager;

import java.text.DateFormat;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

public class ExpressionLanguageConfigTestCase extends AbstractIntegrationTestCase
{
    ExpressionLanguage el;
    ExpressionManager em;

    @Override
    protected String getConfigFile()
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
    public void testExpressionLanguageImport()
    {
        assertEquals(Locale.class, el.evaluate("loc"));
        assertEquals(Locale.class, em.evaluate("loc", null));
    }

    @Test
    public void testExpressionLanguageImportNoName()
    {
        assertEquals(DateFormat.class, el.evaluate("DateFormat"));
        assertEquals(DateFormat.class, em.evaluate("DateFormat", null));
    }

    @Test
    public void testExpressionLanguageAlias()
    {
        assertEquals(muleContext.getConfiguration().getId(), el.evaluate("appName"));
        assertEquals(muleContext.getConfiguration().getId(), em.evaluate("appName", null));
    }

    @Test
    public void testExpressionLanguageGlobalFunction()
    {
        // NOTE: This indirectly asserts that echo() function defined in config file rather than external
        // function definition file is being used (otherwise hiOTHER' would be returned

        assertEquals("hi", el.evaluate("echo('hi')"));
        assertEquals("hi", em.evaluate("echo('hi')", null));
    }

    @Test
    public void testExpressionLanguageGlobalFunctionFromFile()
    {
        assertEquals("hi", el.evaluate("echo2('hi')"));
        assertEquals("hi", em.evaluate("echo2('hi')", null));
    }

    @Test
    public void testExpressionLanguageGlobalFunctionUsingStaticContext()
    {
        assertEquals("Hello " + muleContext.getConfiguration().getId() + "!", el.evaluate("hello()"));
        assertEquals("Hello " + muleContext.getConfiguration().getId() + "!",
            em.evaluate("hello()", null));
    }

    @Test
    public void testExpressionLanguageGlobalFunctionUsingMessageContext() throws Exception
    {
        MuleEvent event = getTestEvent("123");
        assertEquals("123appended", el.evaluate("appendPayload()", event));
        assertEquals("123appended", em.evaluate("appendPayload()", event));
    }

    @Test
    public void testExpressionLanguageGlobalFunctionUsingMessageContextAndImport() throws Exception
    {
        MuleEvent event = getTestEvent("123");
        assertEquals("321", el.evaluate("reversePayload()", event));
        assertEquals("321", em.evaluate("reversePayload()", event));
    }

    @Test
    public void testExpressionLanguageExecuteElement() throws Exception
    {
        flowRunner("flow").withPayload("foo").run();
    }

}
