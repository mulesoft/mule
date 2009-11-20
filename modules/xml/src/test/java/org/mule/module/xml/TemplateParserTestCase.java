/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;

/**
 * @see EE-1734
 */
public class TemplateParserTestCase extends AbstractMuleTestCase
{
    private static final String TEST_MULE_STRING_EXPRESSION_XML = 
        "<xml><t2><tag1 attr1='blahattr1'>BLAH1</tag1><tag1 attr1='blahattr2'>BLAH2</tag1></t2></xml>";

    private MuleMessage message = new DefaultMuleMessage(TEST_MULE_STRING_EXPRESSION_XML);

    public void testXPathExpression() throws Exception
    {
        String result = (String) muleContext.getExpressionManager().evaluate(
            "#[xpath:/xml/t2/tag1[@attr1='blahattr1']]", message);
        assertNotNull(result);
        assertEquals("BLAH1", result);
    }

    public void testStringExpression() throws Exception
    {
        String result = (String) muleContext.getExpressionManager().evaluate(
            "#[string:#[xpath:/xml/t2/tag1[@attr1='blahattr1']]]", message);
        assertNotNull(result);
        assertEquals("BLAH1", result);
    }

    public void testXPathExpressionWithAsterisk() throws Exception
    {
        String result = (String) muleContext.getExpressionManager().evaluate(
            "#[xpath:/xml/*/tag1[@attr1='blahattr1']]", message);
        assertNotNull(result);
        assertEquals("BLAH1", result);
    }

    public void testStringExpressionWithAsterisk() throws Exception
    {
        String result = (String) muleContext.getExpressionManager().evaluate(
            "#[string:#[xpath:/xml/*/tag1[@attr1='blahattr1']]]", message);
        assertNotNull(result);
        assertEquals("BLAH1", result);
    }
}
