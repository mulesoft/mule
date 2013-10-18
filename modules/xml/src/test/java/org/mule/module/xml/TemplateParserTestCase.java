/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
