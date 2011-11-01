/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.StringDataSource;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AttachmentsExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{
    private MuleMessage message;

    @Override
    protected void doSetUp() throws Exception
    {
        try
        {
            Map<String, DataHandler> attachments = new HashMap<String, DataHandler>();
            attachments.put("foo", new DataHandler(new StringDataSource("foovalue")));
            attachments.put("bar", new DataHandler(new StringDataSource("barvalue")));
            attachments.put("baz", new DataHandler(new StringDataSource("bazvalue")));
            message = new DefaultMuleMessage("test", null, attachments, muleContext);

        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }


    @Test
    public void testListAttachments() throws Exception
    {
        MessageAttachmentsListExpressionEvaluator eval = new MessageAttachmentsListExpressionEvaluator();

        Object result = eval.evaluate("foo, baz", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(2, ((List)result).size());

        assertTrue(((List)result).get(0) instanceof DataHandler);
        DataHandler dh = (DataHandler)((List)result).get(0);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("foovalue", baos.toString());

        assertTrue(((List)result).get(1) instanceof DataHandler);
        dh = (DataHandler)((List)result).get(1);
        baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("bazvalue", baos.toString());

        //Test all
        result = eval.evaluate("*", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(3, ((List)result).size());

        result = eval.evaluate("fool?", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(0, ((List)result).size());

        try
        {
            eval.evaluate("fool", message);
            fail("required value");
        }
        catch (Exception e)
        {
            //Expected
        }
    }

    @Test
    public void testListAttachmentsWithWildcards() throws Exception
    {
        MessageAttachmentsListExpressionEvaluator eval = new MessageAttachmentsListExpressionEvaluator();

        //Test All Wildcard
        Object result = eval.evaluate("*", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(3, ((List)result).size());

        //Test Wildcard
        result = eval.evaluate("ba*", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(2, ((List)result).size());

        //Test Wildcard no match
        result = eval.evaluate("x*", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(0, ((List)result).size());

        //Test comma separated Wildcards
         result = eval.evaluate("ba*, f*", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(3, ((List)result).size());
    }


    @Test
    public void testListAttachmentsUsingManager() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[attachments-list:foo,baz]", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(2, ((List)result).size());

        assertTrue(((List)result).get(0) instanceof DataHandler);
        DataHandler dh = (DataHandler)((List)result).get(0);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("foovalue", baos.toString());

        assertTrue(((List)result).get(1) instanceof DataHandler);
        dh = (DataHandler)((List)result).get(1);
        baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("bazvalue", baos.toString());

        result = muleContext.getExpressionManager().evaluate("#[attachments-list:fool?]", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(0, ((List)result).size());

        try
        {
            muleContext.getExpressionManager().evaluate("#[attachments-list:fool]", message);
            fail("Required value");
        }
        catch (ExpressionRuntimeException e)
        {
            //expected
        }
    }

    @Test
    public void testListAttachmentsWithWildcardsUsingManager() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[attachments-list:*]", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(3, ((List)result).size());

        result = muleContext.getExpressionManager().evaluate("#[attachments-list:ba*]", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(2, ((List)result).size());

        result = muleContext.getExpressionManager().evaluate("#[attachments-list:x*]", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(0, ((List)result).size());

        result = muleContext.getExpressionManager().evaluate("#[attachments-list:ba*, f*]", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(3, ((List)result).size());
    }
}
