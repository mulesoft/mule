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
import org.mule.api.expression.RequiredValueException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.StringDataSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MessageAttachmentExpressionEvaluatorTestCase extends AbstractMuleContextTestCase
{
    private MessageAttachmentExpressionEvaluator evaluator = new MessageAttachmentExpressionEvaluator();
    private MuleMessage message;

    public MessageAttachmentExpressionEvaluatorTestCase()
    {
        super();
        setDisposeContextPerClass(true);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        Map<String, DataHandler> attachments = createAttachmentsMap();
        message = new DefaultMuleMessage(TEST_MESSAGE, null, attachments, muleContext);
    }

    private Map<String, DataHandler> createAttachmentsMap()
    {
        Map<String, DataHandler> attachments = new HashMap<String, DataHandler>();
        attachments.put("foo", new DataHandler(new StringDataSource("foovalue")));
        attachments.put("bar", new DataHandler(new StringDataSource("barvalue")));
        attachments.put("baz", new DataHandler(new StringDataSource("bazvalue")));
        return attachments;
    }

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

    private void assertAttachmentValueEquals(String expected, Object attachment) throws IOException
    {
        assertTrue(attachment instanceof DataHandler);
        DataHandler dataHandler = (DataHandler) attachment;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataHandler.writeTo(baos);
        assertEquals(expected, baos.toString());
    }
}
