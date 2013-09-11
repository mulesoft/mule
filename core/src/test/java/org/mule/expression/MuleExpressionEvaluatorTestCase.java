/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.transformer.Transformer;
import org.mule.message.DefaultExceptionPayload;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBasket;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.FruitBowlToFruitBasket;
import org.mule.util.StringDataSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;

import org.junit.Test;

public class MuleExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{
    private Map props;

    @Override
    protected void doSetUp() throws Exception
    {
        MuleEvent event = getTestEvent("testing",
                getTestService("apple", Apple.class),
                getTestInboundEndpoint("test", "test://foo"));
        RequestContext.setEvent(event);

        props = new HashMap(3);
        props.put("foo", "moo");
        props.put("bar", "mar");
        props.put("baz", "maz");
    }

    protected MuleMessage createMessageWithAttachments()
    {
        try
        {
            Map<String, DataHandler> attachments = new HashMap<String, DataHandler>();
            attachments.put("foo", new DataHandler(new StringDataSource("moo")));
            attachments.put("bar", new DataHandler(new StringDataSource("mar")));
            attachments.put("baz", new DataHandler(new StringDataSource("maz")));
            return new DefaultMuleMessage("test", null, attachments, muleContext);
        }
        catch (Exception e)
        {
            fail(e.getMessage());
            return null;
        }
    }

    @Test
    public void testSingleAttachment() throws Exception
    {
        MuleExpressionEvaluator eval = new MuleExpressionEvaluator();
        eval.setMuleContext(muleContext);

        Object result = eval.evaluate("message.attachment(foo)", createMessageWithAttachments());
        assertNotNull(result);
        assertTrue(result instanceof DataHandler);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
        ((DataHandler)result).writeTo(baos);
        assertEquals("moo", baos.toString());

        result = eval.evaluate("message.attachment(foo?)", createMessageWithAttachments());
        assertNotNull(result);
        assertTrue(result instanceof DataHandler);
        baos = new ByteArrayOutputStream(4);
        ((DataHandler)result).writeTo(baos);
        assertEquals("moo", baos.toString());        
        
        result = eval.evaluate("message.attachment(fool?)", createMessageWithAttachments());
        assertNull(result);

        try
        {
            eval.evaluate("message.attachments(fool)", createMessageWithAttachments());
            fail("Attachment 'fool' is not on the nessage and not defined as optional");
        }
        catch (Exception e)
        {
            //Expected
        }
    }

    @Test
    public void testMapAttachments() throws Exception
    {
        MuleExpressionEvaluator eval = new MuleExpressionEvaluator();
        eval.setMuleContext(muleContext);

        Object result = eval.evaluate("message.attachments(foo, baz)", createMessageWithAttachments());
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(2, ((Map)result).size());

        assertNotNull(((Map)result).get("foo"));
        assertTrue(((Map)result).get("foo") instanceof DataHandler);
        DataHandler dh = (DataHandler)((Map)result).get("foo");
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("moo", baos.toString());

        assertNotNull(((Map)result).get("baz"));
        assertTrue(((Map)result).get("baz") instanceof DataHandler);
        dh = (DataHandler)((Map)result).get("baz");
        baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("maz", baos.toString());

        result = eval.evaluate("message.attachments(fool?)", createMessageWithAttachments());
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(0, ((Map)result).size());

        result = eval.evaluate("message.attachments(foo?, baz)", createMessageWithAttachments());
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(2, ((Map)result).size());

        assertNotNull(((Map)result).get("foo"));
        assertTrue(((Map)result).get("foo") instanceof DataHandler);
        dh = (DataHandler)((Map)result).get("foo");
        baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("moo", baos.toString());

        assertNotNull(((Map)result).get("baz"));
        assertTrue(((Map)result).get("baz") instanceof DataHandler);
        dh = (DataHandler)((Map)result).get("baz");
        baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("maz", baos.toString());        

        try
        {
            eval.evaluate("message.attachments(fool)", createMessageWithAttachments());
            fail("Attachment 'fool' is not on the nessage and not defined as optional");
        }
        catch (Exception e)
        {
            //Expected
        }
    }

    @Test
    public void testListAttachments() throws Exception
    {
        MuleExpressionEvaluator eval = new MuleExpressionEvaluator();
        eval.setMuleContext(muleContext);        

        Object result = eval.evaluate("message.attachments-list(foo, baz)", createMessageWithAttachments());
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(2, ((List)result).size());

        assertTrue(((List)result).get(0) instanceof DataHandler);
        DataHandler dh = (DataHandler)((List)result).get(0);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("moo", baos.toString());

        assertTrue(((List)result).get(1) instanceof DataHandler);
        dh = (DataHandler)((List)result).get(1);
        baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("maz", baos.toString());

        result = eval.evaluate("message.attachments-list(fool?)", createMessageWithAttachments());
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(0, ((List)result).size());

        result = eval.evaluate("message.attachments-list(foo?, baz)", createMessageWithAttachments());
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(2, ((List)result).size());

        assertTrue(((List)result).get(0) instanceof DataHandler);
        dh = (DataHandler)((List)result).get(0);
        baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("moo", baos.toString());

        assertTrue(((List)result).get(1) instanceof DataHandler);
        dh = (DataHandler)((List)result).get(1);
        baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("maz", baos.toString());        
        
        result = eval.evaluate("message.attachments-list(fool?)", createMessageWithAttachments());
        assertEquals(0, ((List)result).size());

        try
        {
            eval.evaluate("message.attachments-list(fool)", createMessageWithAttachments());
            fail("Attachment 'fool' is not on the message and not defined as optional");
        }
        catch (Exception e)
        {
            //Expected
        }
    }

    @Test
    public void testSingleAttachmentUsingManager() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[mule:message.attachment(foo)]", createMessageWithAttachments());
        assertNotNull(result);
        assertTrue(result instanceof DataHandler);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
        ((DataHandler)result).writeTo(baos);
        assertEquals("moo", baos.toString());

        result = muleContext.getExpressionManager().evaluate("#[mule:message.attachment(foo?)]", createMessageWithAttachments());
        assertNotNull(result);
        assertTrue(result instanceof DataHandler);
        baos = new ByteArrayOutputStream(4);
        ((DataHandler)result).writeTo(baos);
        assertEquals("moo", baos.toString());        
        
        result = muleContext.getExpressionManager().evaluate("#[mule:message.attachment(fool?)]", createMessageWithAttachments());
        assertNull(result);
        try
        {
            muleContext.getExpressionManager().evaluate("#[mule:message.attachment(fool)]", createMessageWithAttachments());
            fail("Attachment 'fool' is not on the message and not defined as optional");
        }
        catch (Exception e)
        {
            //Expected
        }
    }

    @Test
    public void testMapAttachmentsUsingManager() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[mule:message.attachments(foo, baz)]", createMessageWithAttachments());
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(2, ((Map)result).size());

        assertNotNull(((Map)result).get("foo"));
        assertTrue(((Map)result).get("foo") instanceof DataHandler);
        DataHandler dh = (DataHandler)((Map)result).get("foo");
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("moo", baos.toString());

        assertNotNull(((Map)result).get("baz"));
        assertTrue(((Map)result).get("baz") instanceof DataHandler);
        dh = (DataHandler)((Map)result).get("baz");
        baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("maz", baos.toString());

        result = muleContext.getExpressionManager().evaluate("#[mule:message.attachments(foo?, baz)]", createMessageWithAttachments());
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(2, ((Map)result).size());

        assertNotNull(((Map)result).get("foo"));
        assertTrue(((Map)result).get("foo") instanceof DataHandler);
        dh = (DataHandler)((Map)result).get("foo");
        baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("moo", baos.toString());

        assertNotNull(((Map)result).get("baz"));
        assertTrue(((Map)result).get("baz") instanceof DataHandler);
        dh = (DataHandler)((Map)result).get("baz");
        baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("maz", baos.toString());        
        
        result = muleContext.getExpressionManager().evaluate("#[mule:message.attachments(foo, fool?)]", createMessageWithAttachments());
        assertNotNull(((Map)result).get("foo"));

        try
        {
             muleContext.getExpressionManager().evaluate("#[mule:message.attachments(foo, fool)]", createMessageWithAttachments());
            fail("Attachment 'fool' is not on the message and not defined as optional");
        }
        catch (Exception e)
        {
            //Expected
        }
    }

    @Test
    public void testListAttachmentsUsingManager() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[mule:message.attachments-list(foo,baz)]", createMessageWithAttachments());
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(2, ((List)result).size());

        assertTrue(((List)result).get(0) instanceof DataHandler);
        DataHandler dh = (DataHandler)((List)result).get(0);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("moo", baos.toString());

        assertTrue(((List)result).get(1) instanceof DataHandler);
        dh = (DataHandler)((List)result).get(1);
        baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("maz", baos.toString());

        result = muleContext.getExpressionManager().evaluate("#[mule:message.attachments-list(fool?)]", createMessageWithAttachments());
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(0, ((List)result).size());

        result = muleContext.getExpressionManager().evaluate("#[mule:message.attachments-list(foo?,baz)]", createMessageWithAttachments());
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(2, ((List)result).size());

        assertTrue(((List)result).get(0) instanceof DataHandler);
        dh = (DataHandler)((List)result).get(0);
        baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("moo", baos.toString());

        assertTrue(((List)result).get(1) instanceof DataHandler);
        dh = (DataHandler)((List)result).get(1);
        baos = new ByteArrayOutputStream(4);
        dh.writeTo(baos);
        assertEquals("maz", baos.toString());        
        
        result = muleContext.getExpressionManager().evaluate("#[mule:message.attachments-list(fool?)]", createMessageWithAttachments());
        assertEquals(0, ((List)result).size());

        result = muleContext.getExpressionManager().evaluate("#[mule:message.attachments-list(foo, fool?)]", createMessageWithAttachments());
        assertTrue(((List)result).get(0) instanceof DataHandler);
        assertEquals(1, ((List)result).size());

        try
        {
            muleContext.getExpressionManager().evaluate("#[mule:message.attachments-list(foo, fool)]", createMessageWithAttachments());
            fail("Attachment 'fool' is not on the message and not defined as optional");
        }
        catch (Exception e)
        {
            //Expected
        }
    }

    @Test
    public void testGettingAllAttachments() throws Exception
    {
        MuleExpressionEvaluator eval = new MuleExpressionEvaluator();
        eval.setMuleContext(muleContext);

        Object result = eval.evaluate("message.attachments(*)", createMessageWithAttachments());
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(3, ((Map)result).size());

        result = eval.evaluate("message.attachments-list(*)", createMessageWithAttachments());
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(3, ((List)result).size());

    }

    @Test
    public void testGettingAllAttachmentsUsingManager() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[mule:message.attachments(*)]", createMessageWithAttachments());
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(3, ((Map)result).size());

        result = muleContext.getExpressionManager().evaluate("#[mule:message.attachments-list(*)]", createMessageWithAttachments());
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(3, ((List)result).size());

    }

    @Test
    public void testSingleHeader() throws Exception
    {
        MuleExpressionEvaluator eval = new MuleExpressionEvaluator();
        eval.setMuleContext(muleContext);
        MuleMessage message = new DefaultMuleMessage("test", props, muleContext);

        Object result = eval.evaluate("message.header(foo)", message);
        assertNotNull(result);
        assertEquals("moo", result);
        
        result = eval.evaluate("message.header(foo?)", message);
        assertNotNull(result);
        assertEquals("moo", result);

        result = eval.evaluate("message.header(fool?)", message);
        assertNull(result);

        try
        {
            muleContext.getExpressionManager().evaluate("#[mule:message.header(fool)]", createMessageWithAttachments());
            fail("Header 'fool' is not on the message and not defined as optional");
        }
        catch (Exception e)
        {
            //Expected
        }
    }

    @Test
    public void testMapHeaders() throws Exception
    {
        MuleExpressionEvaluator eval = new MuleExpressionEvaluator();
        eval.setMuleContext(muleContext);

        MuleMessage message = new DefaultMuleMessage("test", props, muleContext);

        Object result = eval.evaluate("message.headers(foo, baz)", message);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(2, ((Map)result).size());
        assertTrue(((Map)result).values().contains("moo"));
        assertTrue(((Map)result).values().contains("maz"));
        assertFalse(((Map)result).values().contains("mar"));
        
        result = eval.evaluate("message.headers(foo?, baz)", message);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(2, ((Map)result).size());
        assertTrue(((Map)result).values().contains("moo"));
        assertTrue(((Map)result).values().contains("maz"));
        assertFalse(((Map)result).values().contains("mar"));        

        result = eval.evaluate("message.headers(fool?)", message);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(0, ((Map)result).size());

        result = eval.evaluate("message.headers(foo, fool?)", message);
        assertTrue(result instanceof Map);
        assertEquals("moo", ((Map)result).get("foo"));
        assertEquals(1, ((Map)result).size());

        try
        {
            eval.evaluate("message.headers(foo, fool)", createMessageWithAttachments());
            fail("Header 'fool' is not on the message and not defined as optional");
        }
        catch (Exception e)
        {
            //Expected
        }
    }

    @Test
    public void testListHeaders() throws Exception
    {
        MuleExpressionEvaluator eval = new MuleExpressionEvaluator();
        eval.setMuleContext(muleContext);
        
        MuleMessage message = new DefaultMuleMessage("test", props, muleContext);

        Object result = eval.evaluate("message.headers-list(foo, baz)", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(2, ((List)result).size());
        assertTrue(((List)result).contains("moo"));
        assertTrue(((List)result).contains("maz"));
        assertFalse(((List)result).contains("mar"));

        result = eval.evaluate("message.headers(fool?)", message);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(0, ((Map)result).size());

        result = eval.evaluate("message.headers-list(foo?, baz)", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(2, ((List)result).size());
        assertTrue(((List)result).contains("moo"));
        assertTrue(((List)result).contains("maz"));
        assertFalse(((List)result).contains("mar"));        
        
        result = eval.evaluate("message.headers(fool?)", message);
        assertEquals(0, ((Map)result).size());

        result = eval.evaluate("message.headers-list(foo, fool?)", message);
        assertTrue(result instanceof List);
        assertEquals("moo", ((List)result).get(0));
        assertEquals(1, ((List)result).size());

        try
        {
            eval.evaluate("message.headers-list(foo, fool)", createMessageWithAttachments());
            fail("Header 'fool' is not on the message and not defined as optional");
        }
        catch (Exception e)
        {
            //Expected
        }
    }

    @Test
    public void testGettingAllHeaders() throws Exception
    {
        MuleExpressionEvaluator eval = new MuleExpressionEvaluator();
        eval.setMuleContext(muleContext);

        MuleMessage message = new DefaultMuleMessage("test", props, muleContext);

        Object result = eval.evaluate("message.headers(*)", message);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(3, ((Map)result).size());

        result = eval.evaluate("message.headers-list(*)", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(3, ((List)result).size());
    }

    @Test
    public void testGettingAllHeadersUsingManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", props, muleContext);

        Object result = muleContext.getExpressionManager().evaluate("#[mule:message.headers(*)]", message);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(3, ((Map)result).size());

        result = muleContext.getExpressionManager().evaluate("#[mule:message.headers-list(*)]", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(3, ((List)result).size());
    }

    @Test
    public void testSingleHeaderUsingManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", props, muleContext);

        Object result = muleContext.getExpressionManager().evaluate("#[mule:message.header(foo)]", message);
        assertNotNull(result);
        assertEquals("moo", result);

        result = muleContext.getExpressionManager().evaluate("#[mule:message.header(foo?)]", message);
        assertNotNull(result);
        assertEquals("moo", result);        
        
        result = muleContext.getExpressionManager().evaluate("#[mule:message.header(fool?)]", message);
        assertNull(result);

        try
        {
            muleContext.getExpressionManager().evaluate("#[mule:message.header(fool)]", createMessageWithAttachments());
            fail("Header 'fool' is not on the message and not defined as optional");
        }
        catch (Exception e)
        {
            //Expected
        }
    }

    @Test
    public void testMapHeadersUsingManager() throws Exception
    {

        MuleMessage message = new DefaultMuleMessage("test", props, muleContext);

        Object result = muleContext.getExpressionManager().evaluate("#[mule:message.headers(foo, baz)]", message);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(2, ((Map)result).size());
        assertTrue(((Map)result).values().contains("moo"));
        assertTrue(((Map)result).values().contains("maz"));
        assertFalse(((Map)result).values().contains("mar"));

        result = muleContext.getExpressionManager().evaluate("#[mule:message.headers(fool?)]", message);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(0, ((Map)result).size());

        result = muleContext.getExpressionManager().evaluate("#[mule:message.headers(foo?, baz)]", message);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(2, ((Map)result).size());
        assertTrue(((Map)result).values().contains("moo"));
        assertTrue(((Map)result).values().contains("maz"));
        assertFalse(((Map)result).values().contains("mar"));        
        
        result = muleContext.getExpressionManager().evaluate("#[mule:message.headers(fool?)]", message);
        assertEquals(0, ((Map)result).size());

        result = muleContext.getExpressionManager().evaluate("#[mule:message.headers(foo, fool?)]", message);
        assertTrue(result instanceof Map);
        assertEquals("moo", ((Map)result).get("foo"));
        assertEquals(1, ((Map)result).size());

        try
        {
            muleContext.getExpressionManager().evaluate("#[mule:message.headers(foo, fool)]", createMessageWithAttachments());
            fail("Header 'fool' is not on the message and not defined as optional");
        }
        catch (Exception e)
        {
            //Expected
        }
    }

    @Test
    public void testListHeadersUsingManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", props, muleContext);

        Object result = muleContext.getExpressionManager().evaluate("#[mule:message.headers-list(foo, baz)]", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(2, ((List)result).size());
        assertTrue(((List)result).contains("moo"));
        assertTrue(((List)result).contains("maz"));
        assertFalse(((List)result).contains("mar"));

        result = muleContext.getExpressionManager().evaluate("#[mule:message.headers-list(fool?)]", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(0, ((List)result).size());

        result = muleContext.getExpressionManager().evaluate("#[mule:message.headers-list(foo?, baz)]", message);
        assertNotNull(result);
        assertTrue(result instanceof List);
        assertEquals(2, ((List)result).size());
        assertTrue(((List)result).contains("moo"));
        assertTrue(((List)result).contains("maz"));
        assertFalse(((List)result).contains("mar"));        
        
        result = muleContext.getExpressionManager().evaluate("#[mule:message.headers(fool?)]", message);
        assertEquals(0, ((Map)result).size());

        result = muleContext.getExpressionManager().evaluate("#[mule:message.headers-list(foo, fool?)]", message);
        assertTrue(result instanceof List);
        assertEquals("moo", ((List)result).get(0));
        assertEquals(1, ((List)result).size());

        try
        {
            muleContext.getExpressionManager().evaluate("#[mule:message.headers-list(foo, fool)]", createMessageWithAttachments());
            fail("Header 'fool' is not on the message and not defined as optional");
        }
        catch (Exception e)
        {
            //Expected
        }
    }

    @Test
    public void testContextExpressions() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        MuleExpressionEvaluator extractor = new MuleExpressionEvaluator();
        extractor.setMuleContext(muleContext);

        Object o = extractor.evaluate("context.serviceName", message);
        assertEquals("apple", o);

        o = extractor.evaluate("context.modelName", message);
        assertNotNull(o);

        o = extractor.evaluate("context.inboundEndpoint", message);
        assertEquals("test://foo", o.toString());

        o = extractor.evaluate("context.serverId", message);
        assertNotNull(o);

        o = extractor.evaluate("context.clusterId", message);
        assertNotNull(o);

        o = extractor.evaluate("context.domainId", message);
        assertNotNull(o);

        o = extractor.evaluate("context.workingDir", message);
        assertNotNull(o);

        try
        {
            extractor.evaluate("context.bork", message);
            fail("bork is not a valid mule context value");
        }
        catch (Exception e)
        {
            //expected
        }
    }

    @Test
    public void testContextExpressionsFromExtractorManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        Object o = muleContext.getExpressionManager().evaluate("mule:context.serviceName", message);
        assertEquals("apple", o);

        o = muleContext.getExpressionManager().evaluate("mule:context.modelName", message);
        assertNotNull(o);

        o = muleContext.getExpressionManager().evaluate("mule:context.inboundEndpoint", message);
        assertEquals("test://foo", o.toString());

        o = muleContext.getExpressionManager().evaluate("mule:context.serverId", message);
        assertNotNull(o);

        o = muleContext.getExpressionManager().evaluate("mule:context.clusterId", message);
        assertNotNull(o);

        o = muleContext.getExpressionManager().evaluate("mule:context.domainId", message);
        assertNotNull(o);

        o = muleContext.getExpressionManager().evaluate("mule:context.workingDir", message);
        assertNotNull(o);

        try
        {
            muleContext.getExpressionManager().evaluate("mule:context.bork", message);
            fail("bork is not a valid mule context value");
        }
        catch (Exception e)
        {
            //expected
        }
    }

    @Test
    public void testMissingEventContext() throws Exception
    {
        RequestContext.clear();

        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        MuleExpressionEvaluator extractor = new MuleExpressionEvaluator();
        extractor.setMuleContext(muleContext);

        Object o = extractor.evaluate("context.serverId", message);
        assertNotNull(o);

        try
        {
            extractor.evaluate("context.serviceName", message);
            fail("There is no current event context");
        }
        catch (MuleRuntimeException e)
        {
            //expected
        }
    }

    @Test
    public void testMessagePropertiesUsingEvaluatorDirectly() throws Exception
    {
        MuleExpressionEvaluator eval = new MuleExpressionEvaluator();
        eval.setMuleContext(muleContext);
        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        message.setCorrelationId(message.getUniqueId());
        message.setCorrelationSequence(1);
        message.setCorrelationGroupSize(2);
        message.setReplyTo("foo");
        message.setEncoding("UTF-8");
        Exception e = new Exception("dummy");
        message.setExceptionPayload(new DefaultExceptionPayload(e));

        //no expression
        Object result = eval.evaluate(null, message);
        assertNotNull(result);
        assertEquals(message, result);

        //no expression
        result = eval.evaluate(null, null);
        assertNull(result);

        assertEquals(message.getUniqueId(), eval.evaluate("message.id", message));
        assertEquals(message.getUniqueId(), eval.evaluate("message.correlationId", message));
        assertEquals(new Integer(1), eval.evaluate("message.correlationSequence", message));
        assertEquals(new Integer(2), eval.evaluate("message.correlationGroupSize", message));
        assertEquals("foo", eval.evaluate("message.replyTo", message));
        assertEquals(e, eval.evaluate("message.exception", message));
        assertEquals("UTF-8", eval.evaluate("message.encoding", message));
        assertEquals("test", eval.evaluate("message.payload", message));

        try
        {
            eval.evaluate("message.xxx", message);
            fail("message.xxx is not a supported expresion");
        }
        catch (Exception e1)
        {
            //Expected
        }
    }

    /**
     * Make sure the evaluator gets registered properly
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testMessagePropertiesUsingManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        message.setCorrelationId(message.getUniqueId());
        message.setCorrelationSequence(1);
        message.setCorrelationGroupSize(2);
        message.setReplyTo("foo");
        message.setEncoding("UTF-8");
        Exception e = new Exception("dummy");
        message.setExceptionPayload(new DefaultExceptionPayload(e));


        assertEquals(message.getUniqueId(), muleContext.getExpressionManager().evaluate("#[mule:message.id]", message));
        assertEquals(message.getUniqueId(), muleContext.getExpressionManager().evaluate("#[mule:message.correlationId]", message));
        assertEquals(new Integer(1), muleContext.getExpressionManager().evaluate("#[mule:message.correlationSequence]", message));
        assertEquals(new Integer(2), muleContext.getExpressionManager().evaluate("#[mule:message.correlationGroupSize]", message));
        assertEquals("foo", muleContext.getExpressionManager().evaluate("#[mule:message.replyTo]", message));
        assertEquals(e, muleContext.getExpressionManager().evaluate("#[mule:message.exception]", message));
        assertEquals("UTF-8", muleContext.getExpressionManager().evaluate("#[mule:message.encoding]", message));
        assertEquals("test", muleContext.getExpressionManager().evaluate("#[mule:message.payload]", message));

        try
        {
            muleContext.getExpressionManager().evaluate("#[mule:message.xxx]", message, true);
            fail("xxx is not a supported expresion");
        }
        catch (Exception e1)
        {
            //Expected
        }
    }

    @Test
    public void testMessagePayloadWithNulls() throws Exception
    {
        MuleExpressionEvaluator eval = new MuleExpressionEvaluator();
        eval.setMuleContext(muleContext);

        //no expression
        Object result = eval.evaluate(null, null);
        assertNull(result);
    }

    /**
     * Make sure the evaluator gets registered properly
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testMessagePayloadWithNullsUsingManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", muleContext);

        assertFalse(muleContext.getExpressionManager().isValidExpression("${payload:}"));
        assertTrue(muleContext.getExpressionManager().isValidExpression("#[mule:message.payload]"));

        Object result = muleContext.getExpressionManager().evaluate("#[mule:message.payload]", message);
        assertNotNull(result);
        assertEquals("test", result);

        result = muleContext.getExpressionManager().evaluate("#[mule:message.payload]", (MuleMessage) null);
        assertNull(result);
    }

    @Test
    public void testMessagePayloadWithTransform() throws Exception
    {
        MuleExpressionEvaluator eval = new MuleExpressionEvaluator();
        eval.setMuleContext(muleContext);
        MuleMessage message = new DefaultMuleMessage("test", muleContext);

        //i.e. ${payload:byte[]}
        Object result = eval.evaluate("message.payload(byte[])", message);
        assertNotNull(result);
        assertTrue(result instanceof byte[]);
        assertEquals("test", new String((byte[]) result));

        ByteArrayInputStream bais = new ByteArrayInputStream("test2".getBytes());
        //i.e. ${payload:java.lang.String}
        result = eval.evaluate("message.payload(java.lang.String)", new DefaultMuleMessage(bais, muleContext));
        assertNotNull(result);
        assertEquals("test2", result);
    }

    @Test
    public void testMessagePayloadWithTransformUsingManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", muleContext);

        //i.e. ${payload:byte[]}
        Object result = muleContext.getExpressionManager().evaluate("#[mule:message.payload(byte[])]", message);
        assertNotNull(result);
        assertTrue(result instanceof byte[]);
        assertEquals("test", new String((byte[]) result));

        ByteArrayInputStream bais = new ByteArrayInputStream("test2".getBytes());
        //i.e. ${payload:java.lang.String}
        result = muleContext.getExpressionManager().evaluate("#[mule:message.payload(java.lang.String)]", new DefaultMuleMessage(bais, muleContext));
        assertNotNull(result);
        assertEquals("test2", result);
    }

    @Test
    public void testMessagePayloadWithMoreComplexTransform() throws Exception
    {
        MuleExpressionEvaluator eval = new MuleExpressionEvaluator();
        eval.setMuleContext(muleContext);
        MuleMessage message = new DefaultMuleMessage(new FruitBowl(new Apple(), new Banana()), muleContext);

        //Lets register our transformer so Mule can find it
        muleContext.getRegistry().registerTransformer(new FruitBowlToFruitBasket());

        //i.e. ${payload:org.mule.tck.testmodels.fruit.FruitBasket}
        Object result = eval.evaluate("message.payload(org.mule.tck.testmodels.fruit.FruitBasket)", message);
        assertNotNull(result);
        assertTrue(result instanceof FruitBasket);
        FruitBasket fb = (FruitBasket) result;
        assertEquals(2, fb.getFruit().size());
        assertTrue(fb.hasBanana());
        assertTrue(fb.hasApple());
    }

    @Test
    public void testMessagePayloadWithMoreComplexTransformUsingManager() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(new FruitBowl(new Apple(), new Banana()), muleContext);

        //Lets register our transformer so Mule can find it
        muleContext.getRegistry().registerTransformer(new FruitBowlToFruitBasket());

        //i.e. ${payload:org.mule.tck.testmodels.fruit.FruitBasket}
        Object result = muleContext.getExpressionManager().evaluate("#[mule:message.payload(org.mule.tck.testmodels.fruit.FruitBasket)]", message);
        assertNotNull(result);
        assertTrue(result instanceof FruitBasket);
        FruitBasket fb = (FruitBasket) result;
        assertEquals(2, fb.getFruit().size());
        assertTrue(fb.hasBanana());
        assertTrue(fb.hasApple());
    }

    @Test
    public void testMapPayloadUsingManager() throws Exception
    {
        Map map = new HashMap(1);
        map.put("foo", "far");
        map.put("boo", "bar");
        map.put("zoo", "zar");

        MuleMessage message = new DefaultMuleMessage(map, muleContext);

        assertTrue(muleContext.getExpressionManager().isValidExpression("#[mule:message.map-payload(foo)]"));

        Object result = muleContext.getExpressionManager().evaluate("#[mule:message.map-payload(foo)]", message);
        assertNotNull(result);
        assertEquals("far", result);
        
        result = muleContext.getExpressionManager().evaluate("#[mule:message.map-payload(foo?)]", message);
        assertNotNull(result);
        assertEquals("far", result);

        result = muleContext.getExpressionManager().evaluate("#[mule:message.map-payload(foot?)]", message);
        assertNull(result);

        try
        {
            muleContext.getExpressionManager().evaluate("#[mule:message.map-payload(fool)]", message);
            fail("Map payload does not contain property 'fool' but it is required");
        }
        catch (ExpressionRuntimeException e)
        {
            //Expected
        }


        result = muleContext.getExpressionManager().evaluate("#[mule:message.map-payload(foo, boo)]", message);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(2, ((Map)result).size());

        result = muleContext.getExpressionManager().evaluate("#[mule:message.map-payload(foo?, boo)]", message);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(2, ((Map)result).size());        
        
        result = muleContext.getExpressionManager().evaluate("#[mule:message.map-payload(fool?, boo)]", message);
        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(1, ((Map)result).size());

        try
        {
            muleContext.getExpressionManager().evaluate("#[mule:message.map-payload(fool, boo)]", message);
            fail("Map payload does not contain property 'fool' but it is required");
        }
        catch (ExpressionRuntimeException e)
        {
            //Expected
        }
    }

    @Test
    public void testSimpleRegistryLookup() throws Exception
    {
        FruitBowlToFruitBasket trans = new FruitBowlToFruitBasket();
        trans.setName("bowlToBasket");
        muleContext.getRegistry().registerTransformer(trans);

        MuleMessage message = new DefaultMuleMessage(new Apple(), muleContext);
        RegistryExpressionEvaluator eval = new RegistryExpressionEvaluator();
        eval.setMuleContext(muleContext);
        Object o = eval.evaluate("bowlToBasket", message);
        assertNotNull(o);
        assertTrue(o instanceof Transformer);

        o = eval.evaluate("XXbowlToBasket*", message);
        assertNull(o);

        try
        {
            eval.evaluate("XXbowlToBasket", message);
            fail("Object is not optional");
        }
        catch (Exception e)
        {
            //expected
        }

        //We can't test bean properties since it requires have the XML module on the classpath
    }

    @Test
    public void testSimpleRegistryLookupUsingMAnager() throws Exception
    {
        FruitBowlToFruitBasket trans = new FruitBowlToFruitBasket();
        trans.setName("bowlToBasket");
        muleContext.getRegistry().registerTransformer(trans);

        MuleMessage message = new DefaultMuleMessage(new Apple(), muleContext);
        Object o = muleContext.getExpressionManager().evaluate("#[mule:registry.bowlToBasket]", message);
        assertNotNull(o);
        assertTrue(o instanceof Transformer);

        o = muleContext.getExpressionManager().evaluate("#[mule:registry.XXbowlToBasket*]", message);
        assertNull(o);

        try
        {
            muleContext.getExpressionManager().evaluate("#[mule:registry.XXbowlToBasket]", message);
            fail("Object is not optional");
        }
        catch (Exception e)
        {
            //expected
        }
    }

}
