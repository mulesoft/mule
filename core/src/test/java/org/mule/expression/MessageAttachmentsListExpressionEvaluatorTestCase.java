/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.api.expression.RequiredValueException;
import org.mule.util.ArrayUtils;

import java.io.IOException;
import java.util.List;

import javax.activation.DataHandler;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MessageAttachmentsListExpressionEvaluatorTestCase extends AbstractAttachmentsTestCase
{
    private MessageAttachmentsListExpressionEvaluator evaluator = new MessageAttachmentsListExpressionEvaluator();

    @Test
    public void requiredKeysWithExistingAttachmentsShouldReturnAttachments() throws Exception
    {
        Object result = evaluator.evaluate("foo, baz", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertAttachmentAtIndexHasValue(0, "foovalue", list);
        assertAttachmentAtIndexHasValue(1, "bazvalue", list);
    }

    @Test
    public void requiredKeysWithExistingAttachmentsViaExpressionManagerShouldReturnAttachments() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[attachments-list:foo, baz]", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertAttachmentAtIndexHasValue(0, "foovalue", list);
        assertAttachmentAtIndexHasValue(1, "bazvalue", list);
    }

    @Test(expected = RequiredValueException.class)
    public void requiredKeysWithMissingAttachmentsShouldFail() throws Exception
    {
        evaluator.evaluate("nonexistent", message);
    }

    @Test(expected = RequiredValueException.class)
    public void requiredKeysWithMissingAttachmentsViaExpressionManagerShouldFail() throws Exception
    {
        muleContext.getExpressionManager().evaluate("#[attachments-list:nonexistent]", message);
    }

    @Test
    public void optionalKeysWithExistingAttachmentsShouldReturnAttachments() throws Exception
    {
        Object result = evaluator.evaluate("foo?, bar?", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertAttachmentAtIndexHasValue(0, "foovalue", list);
        assertAttachmentAtIndexHasValue(1, "barvalue", list);
    }

    @Test
    public void optionalKeysWithExistingAttachmentsViaExpressionManagerShouldReturnAttachments() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[attachments-list:foo?, bar?]", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertAttachmentAtIndexHasValue(0, "foovalue", list);
        assertAttachmentAtIndexHasValue(1, "barvalue", list);
    }

    @Test
    public void optionalKeysWithMissingAttachmentsShouldReturnEmptyList() throws Exception
    {
        Object result = evaluator.evaluate("nonexistent?", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(0, list.size());
    }

    @Test
    public void optionalKeysWithMissingAttachmentsViaExpressionManagerShouldReturnEmptyList() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[attachments-list:nonexistent?]", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(0, list.size());
    }

    @Test
    public void matchAllWildcardShouldReturnAllAttachments() throws Exception
    {
        Object result = evaluator.evaluate("*", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertAttachmentsContain(list, "foovalue", "bazvalue", "barvalue");
    }

    @Test
    public void matchAllWildcardViaExpressionManagerShouldReturnAllAttachments() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[attachments-list:*]", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertAttachmentsContain(list, "foovalue", "bazvalue", "barvalue");
    }

    @Test
    public void matchBeginningWildcardShouldReturnAttachments() throws Exception
    {
        Object result = evaluator.evaluate("ba*", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertAttachmentsContain(list, "bazvalue", "barvalue");
    }

    @Test
    public void matchBeginningWildcardViaExpressionManagerShouldReturnAttachments() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[attachments-list:ba*]", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(2, list.size());
        assertAttachmentsContain(list, "bazvalue", "barvalue");
    }

    @Test
    public void wildcardWithNoMatchShouldReturnEmptyList()
    {
        Object result = evaluator.evaluate("x*", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(0, list.size());
    }

    @Test
    public void wildcardWithNoMatchViaExpressionManagerShouldReturnEmptyList()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[attachments-list:x*]", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(0, list.size());
    }

    @Test
    public void multipleWildcardsShouldReturnValues() throws Exception
    {
        Object result = evaluator.evaluate("ba*, f*", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertAttachmentsContain(list, "foovalue", "bazvalue", "barvalue");
    }

    @Test
    public void multipleWildcardsViaExpressionManagerShouldReturnValues() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[attachments-list:ba*, f*]", message);
        assertTrue(result instanceof List);

        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertAttachmentsContain(list, "foovalue", "bazvalue", "barvalue");
    }

    private void assertAttachmentAtIndexHasValue(int index, String expectedValue, List<?> list) throws IOException
    {
        Object attachment = list.get(index);
        assertNotNull(attachment);
        assertAttachmentValueEquals(expectedValue, attachment);
    }

    private void assertAttachmentsContain(List<?> list, String... expected) throws IOException
    {
        for (Object object : list)
        {
            String attachmentString = attachmentToString((DataHandler) object);
            assertTrue(ArrayUtils.contains(expected, attachmentString));
        }
    }
}
