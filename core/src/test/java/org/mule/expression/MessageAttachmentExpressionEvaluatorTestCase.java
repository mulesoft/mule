/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.api.expression.RequiredValueException;

import org.junit.Test;

import static org.junit.Assert.assertNull;

public class MessageAttachmentExpressionEvaluatorTestCase extends AbstractAttachmentsTestCase
{
    private MessageAttachmentExpressionEvaluator evaluator = new MessageAttachmentExpressionEvaluator();

    @Test
    public void requiredKeyWithExistingAttachmentShouldReturnAttachment() throws Exception
    {
        Object result = evaluator.evaluate("foo", message);
        assertAttachmentValueEquals("foovalue", result);
    }

    @Test
    public void requiredKeyWithExistingAttachmentViaExpressionManagerShouldReturnAttachment() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[attachment:foo]", message);
        assertAttachmentValueEquals("foovalue", result);
    }

    @Test(expected = RequiredValueException.class)
    public void requiredKeyWithMissingAttachmentShouldFail()
    {
        evaluator.evaluate("nonexistent", message);
    }

    @Test(expected = RequiredValueException.class)
    public void requiredKeyWithMissingAttachmentViaExpressionManagerShouldFail()
    {
        muleContext.getExpressionManager().evaluate("#[attachment:nonexistent]", message);
    }

    @Test
    public void optionalKeyWithExistingValueShouldReturnAttachment() throws Exception
    {
        Object result = evaluator.evaluate("foo?", message);
        assertAttachmentValueEquals("foovalue", result);
    }

    @Test
    public void optionalKeyWithExistingValueViaExpressionManagerShouldReturnAttachment() throws Exception
    {
        Object result = muleContext.getExpressionManager().evaluate("#[attachment:foo?]", message);
        assertAttachmentValueEquals("foovalue", result);
    }

    @Test
    public void optionalKeyWithMissingValueShouldReturnNull()
    {
        Object result = evaluator.evaluate("nonexistent?", message);
        assertNull(result);
    }

    @Test
    public void optionalKeyWithMissingValueViaExpressionManagerShouldReturnNull()
    {
        Object result = muleContext.getExpressionManager().evaluate("#[attachment:nonexistent?]", message);
        assertNull(result);
    }
}
