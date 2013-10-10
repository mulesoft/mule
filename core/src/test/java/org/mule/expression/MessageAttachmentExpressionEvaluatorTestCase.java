/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
