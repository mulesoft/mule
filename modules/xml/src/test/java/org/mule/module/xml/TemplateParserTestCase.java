/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * see EE-1734
 */
public class TemplateParserTestCase extends AbstractMuleContextTestCase
{
    private static final String TEST_MULE_STRING_EXPRESSION_XML = 
        "<xml><t2><tag1 attr1='blahattr1'>BLAH1</tag1><tag1 attr1='blahattr2'>BLAH2</tag1></t2></xml>";

    private MuleMessage message;
    
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        
        message = new DefaultMuleMessage(TEST_MULE_STRING_EXPRESSION_XML, muleContext);
    }

    @Test
    public void testXPathExpression() throws Exception
    {
        String result = (String) muleContext.getExpressionManager().evaluate(
            "#[xpath:/xml/t2/tag1[@attr1='blahattr1']]", message);
        assertNotNull(result);
        assertEquals("BLAH1", result);
    }

    @Test
    public void testStringExpression() throws Exception
    {
        String result = (String) muleContext.getExpressionManager().evaluate(
            "#[string:#[xpath:/xml/t2/tag1[@attr1='blahattr1']]]", message);
        assertNotNull(result);
        assertEquals("BLAH1", result);
    }

    @Test
    public void testXPathExpressionWithAsterisk() throws Exception
    {
        String result = (String) muleContext.getExpressionManager().evaluate(
            "#[xpath:/xml/*/tag1[@attr1='blahattr1']]", message);
        assertNotNull(result);
        assertEquals("BLAH1", result);
    }

    @Test
    public void testStringExpressionWithAsterisk() throws Exception
    {
        String result = (String) muleContext.getExpressionManager().evaluate(
            "#[string:#[xpath:/xml/*/tag1[@attr1='blahattr1']]]", message);
        assertNotNull(result);
        assertEquals("BLAH1", result);
    }

    @Test
    public void testStringExpressionDoParse() throws Exception
    {
        String result = muleContext.getExpressionManager().parse(
            "#[xpath:/xml/*/tag1[@attr1='blahattr1']]", message);
        assertNotNull(result);
        assertEquals("BLAH1", result);
    }

    @Test
    public void testStringExpressionDoParseEmbedded() throws Exception
    {
        String result = muleContext.getExpressionManager().parse(
            "#[xpath:/xml/*/tag1[@attr1='blahattr1']] foo #[xpath:/xml/*/tag1[@attr1='blahattr2']]", message);
        assertNotNull(result);
        assertEquals("BLAH1 foo BLAH2", result);
    }
}
