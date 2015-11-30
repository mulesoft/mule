/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.types.TypedValue;

import org.junit.Before;
import org.junit.Test;

public class ExpressionConfigTestCase extends AbstractMuleContextTestCase
{

    private DefaultExpressionManager expressionManager;

    @Before
    public void setup() throws InitialisationException
    {
        expressionManager = new DefaultExpressionManager();
        expressionManager.setMuleContext(muleContext);
        expressionManager.registerEvaluator(new MessageHeaderExpressionEvaluator());
        expressionManager.registerEvaluator(new MessageHeadersExpressionEvaluator());
        expressionManager.registerEvaluator(new MessageAttachmentExpressionEvaluator());
        expressionManager.registerEvaluator(new MessageAttachmentsExpressionEvaluator());
        expressionManager.initialise();
    }

    @Test
    public void testConfig() throws Exception
    {
        ExpressionConfig config = new ExpressionConfig("foo=bar", "header", null, "$[", "]");

        assertEquals("$[header:foo=bar]", config.getFullExpression(expressionManager));

        config = new ExpressionConfig("foo,bar", "headers", null);

        assertEquals("#[headers:foo,bar]", config.getFullExpression(expressionManager));

        config = new ExpressionConfig();
        config.parse("#[attachment:baz]");

        assertEquals("attachment", config.getEvaluator());
        assertEquals("baz", config.getExpression());
        assertNull(config.getCustomEvaluator());
    }

    @Test
    public void testCustomConfig() throws Exception
    {
        expressionManager.registerEvaluator(new ExpressionEvaluator()
        {
            public Object evaluate(String expression, MuleMessage message)
            {
                return null;
            }

            @Override
            public TypedValue evaluateTyped(String expression, MuleMessage message)
            {
                return null;
            }

            public void setName(String name)
            {
            }

            public String getName()
            {
                return "customEval";
            }
        });

        ExpressionConfig config = new ExpressionConfig("foo,bar", "custom", "customEval");

        assertEquals("#[customEval:foo,bar]", config.getFullExpression(expressionManager));
    }

    @Test
    public void testExpressionOnlyConfig() throws Exception
    {
        ExpressionConfig config = new ExpressionConfig("header:foo=bar", null, null, "$[", "]");

        assertEquals("$[header:foo=bar]", config.getFullExpression(expressionManager));

        config = new ExpressionConfig("headers:foo,bar", null, null);

        assertEquals("#[headers:foo,bar]", config.getFullExpression(expressionManager));

        config = new ExpressionConfig();
        config.parse("#[attachment:baz]");

        assertEquals("attachment", config.getEvaluator());
        assertEquals("baz", config.getExpression());
        assertNull(config.getCustomEvaluator());
    }

    @Test
    public void testExpressionLanguageExpression() throws Exception
    {
        ExpressionConfig config = new ExpressionConfig("message.inboundProperty['foo']=='bar'", null, null,
            "$[", "]");

        assertEquals("$[message.inboundProperty['foo']=='bar']", config.getFullExpression(expressionManager));

        config = new ExpressionConfig();
        config.parse("#[message.inboundAttachment['baz']]");

        assertEquals(null, config.getEvaluator());
        assertEquals("message.inboundAttachment['baz']", config.getExpression());
        assertNull(config.getCustomEvaluator());
    }

    @Test
    public void testEvaluatorExpressionOnly()
    {
        ExpressionConfig expressionConfig = new ExpressionConfig();
        expressionConfig.setExpression("header:foo");
        assertEquals("header", expressionConfig.getEvaluator());
        assertNull(expressionConfig.getCustomEvaluator());
        assertEquals("foo", expressionConfig.getExpression());
        assertEquals("#[header:foo]", expressionConfig.getFullExpression(expressionManager));
        expressionConfig.validate(expressionManager);
    }

    @Test
    public void testEvaluatorExpressionOnlyWithBrackets()
    {
        ExpressionConfig expressionConfig = new ExpressionConfig();
        expressionConfig.setExpression("#[header:foo]");
        assertEquals("header", expressionConfig.getEvaluator());
        assertNull(expressionConfig.getCustomEvaluator());
        assertEquals("foo", expressionConfig.getExpression());
        assertEquals("#[header:foo]", expressionConfig.getFullExpression(expressionManager));
    }

    @Test
    public void testELExpression()
    {
        ExpressionConfig expressionConfig = new ExpressionConfig();
        expressionConfig.setExpression("message.payload");
        assertNull(expressionConfig.getEvaluator());
        assertNull(expressionConfig.getCustomEvaluator());
        assertEquals("message.payload", expressionConfig.getExpression());
        assertEquals("#[message.payload]", expressionConfig.getFullExpression(expressionManager));
    }

    @Test
    public void testELExpressionWithBrackets()
    {
        ExpressionConfig expressionConfig = new ExpressionConfig();
        expressionConfig.setExpression("#[message.payload]");
        assertNull(expressionConfig.getEvaluator());
        assertNull(expressionConfig.getCustomEvaluator());
        assertEquals("message.payload", expressionConfig.getExpression());
        assertEquals("#[message.payload]", expressionConfig.getFullExpression(expressionManager));
    }

    @Test
    public void testELExpressionWithTenaryIf()
    {
        ExpressionConfig expressionConfig = new ExpressionConfig();
        expressionConfig.setExpression("1==1?true:false");
        assertNull(expressionConfig.getEvaluator());
        assertNull(expressionConfig.getCustomEvaluator());
        assertEquals("1==1?true:false", expressionConfig.getExpression());
        assertEquals("#[1==1?true:false]", expressionConfig.getFullExpression(expressionManager));
    }

    @Test
    public void testELExpressionWithForeach()
    {
        ExpressionConfig expressionConfig = new ExpressionConfig();
        expressionConfig.setExpression("for(a:[1,2,3){'1'}");
        assertNull(expressionConfig.getEvaluator());
        assertNull(expressionConfig.getCustomEvaluator());
        assertEquals("for(a:[1,2,3){'1'}", expressionConfig.getExpression());
        assertEquals("#[for(a:[1,2,3){'1'}]", expressionConfig.getFullExpression(expressionManager));
    }

    @Test
    public void testELExpressionWithColonInString()
    {
        ExpressionConfig expressionConfig = new ExpressionConfig();
        expressionConfig.setExpression("'This is a message : msg'");
        assertNull(expressionConfig.getEvaluator());
        assertNull(expressionConfig.getCustomEvaluator());
        assertEquals("'This is a message : msg'", expressionConfig.getExpression());
        assertEquals("#['This is a message : msg']", expressionConfig.getFullExpression(expressionManager));
    }

    @Test
    public void testXPathExpressionWithNamesapce()
    {
        ExpressionConfig expressionConfig = new ExpressionConfig();
        expressionConfig.setExpression("//this:other/@attr");
        expressionConfig.setEvaluator("header");
        assertEquals("header", expressionConfig.getEvaluator());
        assertNull(expressionConfig.getCustomEvaluator());
        assertEquals("//this:other/@attr", expressionConfig.getExpression());
        assertEquals("#[header://this:other/@attr]", expressionConfig.getFullExpression(expressionManager));
    }

    @Test
    public void testXPathExpressionWithNamesapce2()
    {
        ExpressionConfig expressionConfig = new ExpressionConfig();
        expressionConfig.setEvaluator("header");
        expressionConfig.setExpression("//this:other/@attr");
        assertEquals("header", expressionConfig.getEvaluator());
        assertNull(expressionConfig.getCustomEvaluator());
        assertEquals("//this:other/@attr", expressionConfig.getExpression());
        assertEquals("#[header://this:other/@attr]", expressionConfig.getFullExpression(expressionManager));
    }

    @Test
    public void testNestedExpression()
    {
        muleContext.getExpressionManager().registerEvaluator(new StringExpressionEvaluator());

        ExpressionConfig expressionConfig = new ExpressionConfig();
        expressionConfig.setExpression("#[header:#[header:foo]]");
        assertEquals("header", expressionConfig.getEvaluator());
        assertNull(expressionConfig.getCustomEvaluator());
        assertEquals("#[header:foo]", expressionConfig.getExpression());
        assertEquals("#[header:#[header:foo]]", expressionConfig.getFullExpression(expressionManager));
    }

    @Test
    public void testSetExpressionTwice()
    {
        muleContext.getExpressionManager().registerEvaluator(new StringExpressionEvaluator());

        ExpressionConfig expressionConfig = new ExpressionConfig();
        expressionConfig.setExpression("#[header:this]");
        expressionConfig.setExpression("#[header:other]");
        assertEquals("header", expressionConfig.getEvaluator());
        assertNull(expressionConfig.getCustomEvaluator());
        assertEquals("other", expressionConfig.getExpression());
        assertEquals("#[header:other]", expressionConfig.getFullExpression(expressionManager));
    }

}
