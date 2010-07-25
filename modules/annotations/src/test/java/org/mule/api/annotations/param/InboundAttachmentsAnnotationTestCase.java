/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.param;

import org.mule.api.MuleEventContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.model.EntryPointResolver;
import org.mule.api.model.InvocationResult;
import org.mule.impl.model.resolvers.AnnotatedEntryPointResolver;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.IOUtils;
import org.mule.util.StringDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;

public class InboundAttachmentsAnnotationTestCase extends AbstractMuleTestCase
{
    private InboundAttachmentsAnnotationComponent component;
    private MuleEventContext eventContext;

    @Override
    public void doSetUp() throws Exception
    {
        super.doSetUp();

        eventContext = getTestEventContext("test");

        try
        {
            eventContext.getMessage().addAttachment("foo", new DataHandler(new StringDataSource("fooValue")));
            eventContext.getMessage().addAttachment("bar", new DataHandler(new StringDataSource("barValue")));
            eventContext.getMessage().addAttachment("baz", new DataHandler(new StringDataSource("bazValue")));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
        component = new InboundAttachmentsAnnotationComponent();
    }

    public void testSingleAttachment() throws Exception
    {
        InvocationResult response = doTest("processAttachment", eventContext);
        assertTrue(response.getResult() instanceof DataHandler);
        assertEquals("fooValue", readAttachment((DataHandler) response.getResult()));
    }


    public void testSingleAttachmentWithType() throws Exception
    {
        //These should really be in core, but the @Transformer annotation is not in core
        muleContext.getRegistry().registerObject("dataHandlerTransformers", new DataHandlerTransformer());

        InvocationResult response = doTest("processAttachmentWithType", eventContext);
        assertTrue(response.getResult() instanceof String);
        assertEquals("fooValue", response.getResult());
    }

    public void testSingleAttachmentOptional() throws Exception
    {
        InvocationResult response = doTest("processAttachmentOptional", eventContext);
        assertTrue(response.getResult() instanceof String);
        assertEquals("faz not set", response.getResult());
    }

    public void testSingleAttachmentWithTypeNoMatchingTransform() throws Exception
    {
        //TODO this test still works because DataHandler.toString() gets called by the ObjectToString transformer
        InvocationResult response = doTest("processAttachmentWithType", eventContext);
        assertTrue(response.getResult() instanceof String);
        //assertEquals("fooValue", response.getResult());
    }

    public void testMapAttachments() throws Exception
    {
        InvocationResult response = doTest("processAttachments", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<String, DataHandler> result = (Map<String, DataHandler>) response.getResult();
        assertEquals(2, result.size());
        assertEquals("fooValue", readAttachment(result.get("foo")));
        assertEquals("barValue", readAttachment(result.get("bar")));
        assertNull(result.get("baz"));
    }

    public void testMapAttachmentsMissing() throws Exception
    {
        eventContext.getMessage().removeAttachment("foo");
        try
        {
            doTest("processAttachments", eventContext);
            fail("Required attachment value is missing");
        }
        catch (RequiredValueException e)
        {
            //expected
        }
    }

    public void testMapSingleAttachment() throws Exception
    {
        InvocationResult response = doTest("processSingleMapAttachment", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<String, DataHandler> result = (Map<String, DataHandler>) response.getResult();
        assertEquals(1, result.size());
        assertEquals("fooValue", readAttachment(result.get("foo")));
        assertNull(result.get("bar"));
        assertNull(result.get("baz"));
    }

    public void testMapAttachmentsOptional() throws Exception
    {
        eventContext.getMessage().removeAttachment("baz");

        InvocationResult response = doTest("processAttachmentsOptional", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<String, DataHandler> result = (Map<String, DataHandler>) response.getResult();
        assertEquals(2, result.size());
        assertEquals("fooValue", readAttachment(result.get("foo")));
        assertEquals("barValue", readAttachment(result.get("bar")));
        assertNull(result.get("baz"));
    }

    public void testMapAttachmentsAllOptional() throws Exception
    {
        eventContext.getMessage().removeAttachment("foo");
        eventContext.getMessage().removeAttachment("bar");
        eventContext.getMessage().removeAttachment("baz");

        InvocationResult response = doTest("processAttachmentsAllOptional", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<String, DataHandler> result = (Map<String, DataHandler>) response.getResult();
        assertEquals(0, result.size());
    }

    public void testMapAttachmentsUnmodifiable() throws Exception
    {
        try
        {
            doTest("processUnmodifiableAttachments", eventContext);
            fail("Required attachment value is missing");
        }
        catch (InvocationTargetException e)
        {
            //expected
            assertTrue(e.getTargetException() instanceof UnsupportedOperationException);
        }
    }

    public void testMapAttachmentsAll() throws Exception
    {
        InvocationResult response = doTest("processAttachmentsAll", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<String, DataHandler> result = (Map<String, DataHandler>) response.getResult();
        //Will include all Mule attachments too
        assertTrue(result.size() >= 3);
        assertEquals("fooValue", readAttachment(result.get("foo")));
        assertEquals("barValue", readAttachment(result.get("bar")));
        assertEquals("bazValue", readAttachment(result.get("baz")));
    }

    public void testMapAttachmentsWildcard() throws Exception
    {
        InvocationResult response = doTest("processAttachmentsWildcard", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<String, DataHandler> result = (Map<String, DataHandler>) response.getResult();
        //Will match on ba*
        assertEquals(2, result.size());
        assertNull(result.get("foo"));
        assertNotNull(result.get("bar"));
        assertNotNull(result.get("baz"));
    }

    public void testMapAttachmentsMultiWildcard() throws Exception
    {
        InvocationResult response = doTest("processAttachmentsMultiWildcard", eventContext);
        assertTrue("Message payload should be a Map", response.getResult() instanceof Map);
        Map<String, DataHandler> result = (Map<String, DataHandler>) response.getResult();
        //Will match on ba*, f*
        assertEquals(3, result.size());

        assertNotNull(result.get("foo"));
        assertNotNull(result.get("bar"));
        assertNotNull(result.get("baz"));
    }

    public void testListAttachments() throws Exception
    {
        InvocationResult response = doTest("processAttachmentsList", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<DataHandler> result = (List<DataHandler>) response.getResult();
        assertEquals(3, result.size());
    }

    public void testListAttachmentsWithOptional() throws Exception
    {
        eventContext.getMessage().removeAttachment("baz");
        InvocationResult response = doTest("processAttachmentsListOptional", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<DataHandler> result = (List<DataHandler>) response.getResult();
        assertEquals(2, result.size());
    }

    public void testListAttachmentsWithAllOptional() throws Exception
    {
        eventContext.getMessage().removeAttachment("foo");
        eventContext.getMessage().removeAttachment("bar");
        eventContext.getMessage().removeAttachment("baz");

        InvocationResult response = doTest("processAttachmentsListAllOptional", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<DataHandler> result = (List<DataHandler>) response.getResult();
        assertEquals(0, result.size());
    }

    public void testListAttachmentsWithMissing() throws Exception
    {
        eventContext.getMessage().removeAttachment("bar");
        try
        {
            doTest("processAttachmentsList", eventContext);
            fail("Required attachment value is missing");
        }
        catch (RequiredValueException e)
        {
            //expected
        }
    }

    public void testSingleListAttachment() throws Exception
    {
        InvocationResult response = doTest("processSingleAttachmentList", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<DataHandler> result = (List<DataHandler>) response.getResult();
        assertEquals(1, result.size());
    }

    public void testListAttachmentsUnmodifiable() throws Exception
    {
        try
        {
            doTest("processUnmodifiableAttachmentsList", eventContext);
            fail("Required attachment value is missing");
        }
        catch (InvocationTargetException e)
        {
            //expected
            assertTrue(e.getTargetException() instanceof UnsupportedOperationException);
        }
    }

    public void testListAttachmentsAll() throws Exception
    {
        InvocationResult response = doTest("processAttachmentsListAll", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<DataHandler> result = (List<DataHandler>) response.getResult();
        assertEquals(3, result.size());
    }

    public void testListAttachmentsWilcard() throws Exception
    {
        InvocationResult response = doTest("processAttachmentsListWildcard", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<DataHandler> result = (List<DataHandler>) response.getResult();
        //Will match all attachments with ba*
        assertEquals(2, result.size());

    }

    public void testListAttachmentsMultiWilcard() throws Exception
    {
        InvocationResult response = doTest("processAttachmentsListMultiWildcard", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<DataHandler> result = (List<DataHandler>) response.getResult();
        //Will match all attachments with ba* and f*
        assertEquals(3, result.size());
    }

    private String readAttachment(DataHandler handler) throws IOException
    {
        return IOUtils.toString((InputStream) handler.getContent());
    }

    protected InvocationResult doTest(String methodName, MuleEventContext eventContext) throws Exception
    {
        EntryPointResolver resolver = getResolver();
        eventContext.getMessage().setInvocationProperty(MuleProperties.MULE_METHOD_PROPERTY, methodName);
        InvocationResult result = resolver.invoke(component, eventContext);
        if (InvocationResult.State.SUCCESSFUL == result.getState())
        {
            assertNotNull("The result of invoking the component should not be null", result.getResult());
            assertNull(result.getErrorMessage());
            assertFalse(result.hasError());
            assertEquals(methodName, result.getMethodCalled());
        }
        return result;
    }

    protected EntryPointResolver getResolver() throws Exception
    {
        return createObject(AnnotatedEntryPointResolver.class);
    }
}
