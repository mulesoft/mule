/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.api.expression.RequiredValueException;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MessageAttachmentsExpressionEvaluatorTestCase extends AbstractAttachmentsTestCase
{
    private MessageAttachmentsExpressionEvaluator evaluator = new MessageAttachmentsExpressionEvaluator();

    @Test
    public void requiredKeysWithExistingAttachmentsShouldReturnAttachments() throws Exception
    {
        Object result = evaluator.evaluate("foo, baz", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.size());
        assertAttachmentWithKeyHasValue("foo", "foovalue", map);
        assertAttachmentWithKeyHasValue("baz", "bazvalue", map);
    }

    @Test
    public void requiredKeysWithExistingAttachmentsViaExpressionManagerShouldReturnAttachments() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[attachments:foo, baz]", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.size());
        assertAttachmentWithKeyHasValue("foo", "foovalue", map);
        assertAttachmentWithKeyHasValue("baz", "bazvalue", map);
    }

    @Test(expected = RequiredValueException.class)
    public void requiredKeysWithMissingAttachmentsShouldFail()
    {
        evaluator.evaluate("nonexistent", message);
    }

    @Test(expected = RequiredValueException.class)
    public void requiredKeysWithMissingAttachmentsViaExpressionManagerShouldFail()
    {
        muleContext.getExpressionManager().evaluate("#[attachments:nonexistent[", message);
    }

    @Test
    public void optionalKeysWithExistingAttachmentsShouldReturnAttachments() throws Exception
    {
        Object result = evaluator.evaluate("foo?,bar?", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.size());
        assertAttachmentWithKeyHasValue("foo", "foovalue", map);
        assertAttachmentWithKeyHasValue("bar", "barvalue", map);
    }

    @Test
    public void optionalKeysWithExistingAttachmentsViaExpressionManagerShouldReturnAttachments() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[attachments:foo?, bar?]", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.size());
        assertAttachmentWithKeyHasValue("foo", "foovalue", map);
        assertAttachmentWithKeyHasValue("bar", "barvalue", map);
    }

    @Test
    public void optionalKeysWithMissingAttachmentsShouldReturnEmptyMap() throws Exception
    {
        Object result = evaluator.evaluate("nonexistent?", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(0, map.size());
    }

    @Test
    public void optionalKeysWithMissingAttachmentsViaExpressionManagerShouldReturnEmptyMap() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[attachments:nonexistent?]", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(0, map.size());
    }

    @Test
    public void matchAllWildcardShouldReturnAllAttachments() throws Exception
    {
        Object result = evaluator.evaluate("*", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(3, map.size());
        assertAttachmentWithKeyHasValue("foo", "foovalue", map);
        assertAttachmentWithKeyHasValue("bar", "barvalue", map);
        assertAttachmentWithKeyHasValue("baz", "bazvalue", map);
    }

    @Test
    public void matchAllWildcardViaExpressionManagerShouldReturnAllAttachments() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[attachments:*]", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(3, map.size());
        assertAttachmentWithKeyHasValue("foo", "foovalue", map);
        assertAttachmentWithKeyHasValue("bar", "barvalue", map);
        assertAttachmentWithKeyHasValue("baz", "bazvalue", map);
    }

    @Test
    public void matchBeginningWildcardShouldReturnAttachments() throws Exception
    {
        Object result = evaluator.evaluate("ba*", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.size());
        assertAttachmentWithKeyHasValue("bar", "barvalue", map);
        assertAttachmentWithKeyHasValue("baz", "bazvalue", map);
    }

    @Test
    public void matchBeginningWildcardViaExpressionManagerShouldReturnAttachments() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[attachments:ba*]", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.size());
        assertAttachmentWithKeyHasValue("bar", "barvalue", map);
        assertAttachmentWithKeyHasValue("baz", "bazvalue", map);
    }

    @Test
    public void wildcardWithNoMatchShouldReturnEmptyMap()
    {
        Object result = evaluator.evaluate("x*", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(0, map.size());
    }

    @Test
    public void wildcardWithNoMatchViaExpressionManagerShouldReturnEmptyMap()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[attachments:x*]", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(0, map.size());
    }

    @Test
    public void multipleWildcardsShouldReturnValues() throws Exception
    {
        Object result = evaluator.evaluate("ba*, f*", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(3, map.size());
        assertAttachmentWithKeyHasValue("foo", "foovalue", map);
        assertAttachmentWithKeyHasValue("bar", "barvalue", map);
        assertAttachmentWithKeyHasValue("baz", "bazvalue", map);
    }

    @Test
    public void multipleWildcardsViaExpressionManagerShouldReturnValues() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[attachments:ba*, f*]", message);
        assertTrue(result instanceof Map);

        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(3, map.size());
        assertAttachmentWithKeyHasValue("foo", "foovalue", map);
        assertAttachmentWithKeyHasValue("bar", "barvalue", map);
        assertAttachmentWithKeyHasValue("baz", "bazvalue", map);
    }

    private void assertAttachmentWithKeyHasValue(String key, String expectedValue, Map<?, ?> map) throws IOException
    {
        Object attachment = map.get(key);
        assertNotNull(attachment);
        assertAttachmentValueEquals(expectedValue, attachment);
    }
}
