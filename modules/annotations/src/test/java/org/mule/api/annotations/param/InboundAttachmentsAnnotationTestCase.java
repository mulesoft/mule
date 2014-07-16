/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.param;

import org.mule.api.expression.RequiredValueException;
import org.mule.api.model.InvocationResult;
import org.mule.util.StringDataSource;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class InboundAttachmentsAnnotationTestCase extends AbstractAnnotatedEntrypointResolverTestCase
{
    @Override
    protected Object getComponent()
    {
        return new InboundAttachmentsAnnotationComponent();
    }

    @Test
    public void testSingleAttachment() throws Exception
    {
        InvocationResult response = invokeResolver("processAttachment", eventContext);
        assertTrue(response.getResult() instanceof DataHandler);
        assertEquals("fooValue", readAttachment((DataHandler) response.getResult()));
    }

    @Test
    public void testSingleAttachmentWithType() throws Exception
    {
        //These should really be in core, but the @Transformer annotation is not in core
        muleContext.getRegistry().registerObject("dataHandlerTransformers", new DataHandlerTransformer());

        InvocationResult response = invokeResolver("processAttachmentWithType", eventContext);
        assertTrue(response.getResult() instanceof String);
        assertEquals("fooValue", response.getResult());
    }

    @Test
    public void testSingleAttachmentOptional() throws Exception
    {
        InvocationResult response = invokeResolver("processAttachmentOptional", eventContext);
        assertTrue(response.getResult() instanceof String);
        assertEquals("faz not set", response.getResult());
    }

    @Test
    public void testSingleAttachmentWithTypeNoMatchingTransform() throws Exception
    {
        //TODO this test still works because DataHandler.toString() gets called by the ObjectToString transformer
        InvocationResult response = invokeResolver("processAttachmentWithType", eventContext);
        assertTrue(response.getResult() instanceof String);
        //assertEquals("fooValue", response.getResult());
    }

    @Test
    public void testMapAttachments() throws Exception
    {
        InvocationResult response = invokeResolver("processAttachments", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<String, DataHandler> result = (Map<String, DataHandler>) response.getResult();
        assertEquals(2, result.size());
        assertEquals("fooValue", readAttachment(result.get("foo")));
        assertEquals("barValue", readAttachment(result.get("bar")));
        assertNull(result.get("baz"));
    }

    @Test
    public void testMapAttachmentsMissing() throws Exception
    {
        //clear attachments
        eventContext = createEventContext(null, new HashMap<String, DataHandler>());
        try
        {
            invokeResolver("processAttachments", eventContext);
            fail("Required attachment value is missing");
        }
        catch (RequiredValueException e)
        {
            //expected
        }
    }

    @Test
    public void testMapSingleAttachment() throws Exception
    {
        InvocationResult response = invokeResolver("processSingleMapAttachment", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<String, DataHandler> result = (Map<String, DataHandler>) response.getResult();
        assertEquals(1, result.size());
        assertEquals("fooValue", readAttachment(result.get("foo")));
        assertNull(result.get("bar"));
        assertNull(result.get("baz"));
    }

    @Test
    public void testMapAttachmentsOptional() throws Exception
    {
        //restrict attachments
        Map<String, DataHandler> attachments = new HashMap<String, DataHandler>();
        attachments.put("foo", new DataHandler(new StringDataSource("fooValue")));
        attachments.put("bar", new DataHandler(new StringDataSource("barValue")));
        eventContext = createEventContext(null, attachments);

        eventContext.getMessage().removeOutboundAttachment("baz");

        InvocationResult response = invokeResolver("processAttachmentsOptional", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<String, DataHandler> result = (Map<String, DataHandler>) response.getResult();
        assertEquals(2, result.size());
        assertEquals("fooValue", readAttachment(result.get("foo")));
        assertEquals("barValue", readAttachment(result.get("bar")));
        assertNull(result.get("baz"));
    }

    @Test
    public void testMapAttachmentsAllOptional() throws Exception
    {
        //clear attachments
        eventContext = createEventContext(null, new HashMap<String, DataHandler>());

        InvocationResult response = invokeResolver("processAttachmentsAllOptional", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<String, DataHandler> result = (Map<String, DataHandler>) response.getResult();
        assertEquals(0, result.size());
    }

    @Test
    public void testMapAttachmentsUnmodifiable() throws Exception
    {
        try
        {
            invokeResolver("processUnmodifiableAttachments", eventContext);
            fail("Required attachment value is missing");
        }
        catch (InvocationTargetException e)
        {
            //expected
            assertTrue(e.getTargetException() instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void testMapAttachmentsAll() throws Exception
    {
        InvocationResult response = invokeResolver("processAttachmentsAll", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<String, DataHandler> result = (Map<String, DataHandler>) response.getResult();
        //Will include all Mule attachments too
        assertTrue(result.size() >= 3);
        assertEquals("fooValue", readAttachment(result.get("foo")));
        assertEquals("barValue", readAttachment(result.get("bar")));
        assertEquals("bazValue", readAttachment(result.get("baz")));
    }

    @Test
    public void testMapAttachmentsWildcard() throws Exception
    {
        InvocationResult response = invokeResolver("processAttachmentsWildcard", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<String, DataHandler> result = (Map<String, DataHandler>) response.getResult();
        //Will match on ba*
        assertEquals(2, result.size());
        assertNull(result.get("foo"));
        assertNotNull(result.get("bar"));
        assertNotNull(result.get("baz"));
    }

    @Test
    public void testMapAttachmentsMultiWildcard() throws Exception
    {
        InvocationResult response = invokeResolver("processAttachmentsMultiWildcard", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<String, DataHandler> result = (Map<String, DataHandler>) response.getResult();
        //Will match on ba*, f*
        assertEquals(3, result.size());

        assertNotNull(result.get("foo"));
        assertNotNull(result.get("bar"));
        assertNotNull(result.get("baz"));
    }

    @Test
    public void testListAttachments() throws Exception
    {
        InvocationResult response = invokeResolver("processAttachmentsList", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<DataHandler> result = (List<DataHandler>) response.getResult();
        assertEquals(3, result.size());
    }

    @Test
    public void testListAttachmentsWithOptional() throws Exception
    {
        Map<String, DataHandler> attachments = new HashMap<String, DataHandler>();
        attachments.put("foo", new DataHandler(new StringDataSource("fooValue")));
        attachments.put("bar", new DataHandler(new StringDataSource("barValue")));
        eventContext = createEventContext(null, attachments);

        InvocationResult response = invokeResolver("processAttachmentsListOptional", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<DataHandler> result = (List<DataHandler>) response.getResult();
        assertEquals(2, result.size());
    }

    @Test
    public void testListAttachmentsWithAllOptional() throws Exception
    {
        eventContext = createEventContext(null, new HashMap<String, DataHandler>());

        InvocationResult response = invokeResolver("processAttachmentsListAllOptional", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<DataHandler> result = (List<DataHandler>) response.getResult();
        assertEquals(0, result.size());
    }

    @Test
    public void testListAttachmentsWithMissing() throws Exception
    {
        Map<String, DataHandler> attachments = new HashMap<String, DataHandler>();
        attachments.put("foo", new DataHandler(new StringDataSource("fooValue")));
        attachments.put("bar", new DataHandler(new StringDataSource("barValue")));
        eventContext = createEventContext(null, attachments);
        try
        {
            invokeResolver("processAttachmentsList", eventContext);
            fail("Required attachment value is missing");
        }
        catch (RequiredValueException e)
        {
            //expected
        }
    }

    @Test
    public void testSingleListAttachment() throws Exception
    {
        InvocationResult response = invokeResolver("processSingleAttachmentList", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<DataHandler> result = (List<DataHandler>) response.getResult();
        assertEquals(1, result.size());
    }

    @Test
    public void testListAttachmentsUnmodifiable() throws Exception
    {
        try
        {
            invokeResolver("processUnmodifiableAttachmentsList", eventContext);
            fail("Required attachment value is missing");
        }
        catch (InvocationTargetException e)
        {
            //expected
            assertTrue(e.getTargetException() instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void testListAttachmentsAll() throws Exception
    {
        InvocationResult response = invokeResolver("processAttachmentsListAll", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<DataHandler> result = (List<DataHandler>) response.getResult();
        assertEquals(3, result.size());
    }

    @Test
    public void testListAttachmentsWilcard() throws Exception
    {
        InvocationResult response = invokeResolver("processAttachmentsListWildcard", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<DataHandler> result = (List<DataHandler>) response.getResult();
        //Will match all attachments with ba*
        assertEquals(2, result.size());

    }

    @Test
    public void testListAttachmentsMultiWilcard() throws Exception
    {
        InvocationResult response = invokeResolver("processAttachmentsListMultiWildcard", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<DataHandler> result = (List<DataHandler>) response.getResult();
        //Will match all attachments with ba* and f*
        assertEquals(3, result.size());
    }
}
