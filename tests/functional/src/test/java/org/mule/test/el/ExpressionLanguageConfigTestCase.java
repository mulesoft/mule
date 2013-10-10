/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.el;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.el.ExpressionLanguage;
import org.mule.api.expression.ExpressionManager;
import org.mule.el.mvel.MVELExpressionLanguage;
import org.mule.tck.junit4.FunctionalTestCase;

import java.text.DateFormat;
import java.util.Locale;

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
    public void testExpressionLanguageImport()
    {
        assertEquals(Locale.class, el.evaluate("loc"));
        assertEquals(Locale.class, em.evaluate("loc", (MuleMessage) null));
    }

    @Test
    public void testExpressionLanguageImportNoName()
    {
        assertEquals(DateFormat.class, el.evaluate("DateFormat"));
        assertEquals(DateFormat.class, em.evaluate("DateFormat", (MuleMessage) null));
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
        // NOTE: This indirectly asserts that echo() function defined in config file rather than external
        // function definition file is being used (otherwise hiOTHER' would be returned

        assertEquals("hi", el.evaluate("echo('hi')"));
        assertEquals("hi", em.evaluate("echo('hi')", (MuleMessage) null));
    }

    @Test
    public void testExpressionLanguageGlobalFunctionFromFile()
    {
        assertEquals("hi", el.evaluate("echo2('hi')"));
        assertEquals("hi", em.evaluate("echo2('hi')", (MuleMessage) null));
    }

    @Test
    public void testExpressionLanguageGlobalFunctionUsingStaticContext()
    {
        assertEquals("Hello " + muleContext.getConfiguration().getId() + "!", el.evaluate("hello()"));
        assertEquals("Hello " + muleContext.getConfiguration().getId() + "!",
            em.evaluate("hello()", (MuleMessage) null));
    }

    @Test
    public void testExpressionLanguageGlobalFunctionUsingMessageContext()
    {
        MuleMessage message = new DefaultMuleMessage("123", muleContext);
        assertEquals("123appended", el.evaluate("appendPayload()", message));
        assertEquals("123appended", em.evaluate("appendPayload()", message));
    }

    @Test
    public void testExpressionLanguageGlobalFunctionUsingMessageContextAndImport()
    {
        MuleMessage message = new DefaultMuleMessage("123", muleContext);
        assertEquals("321", el.evaluate("reversePayload()", message));
        assertEquals("321", em.evaluate("reversePayload()", message));
    }

    @Test
    public void testExpressionLanguageExecuteElement() throws Exception
    {
        testFlow("flow", getTestEvent("foo"));
    }

}
