/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.StringDataSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class AbstractAttachmentsTestCase extends AbstractMuleContextTestCase
{
    protected MuleMessage message;

    public AbstractAttachmentsTestCase()
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

    protected Map<String, DataHandler> createAttachmentsMap()
    {
        Map<String, DataHandler> attachments = new HashMap<String, DataHandler>();
        attachments.put("foo", new DataHandler(new StringDataSource("foovalue")));
        attachments.put("bar", new DataHandler(new StringDataSource("barvalue")));
        attachments.put("baz", new DataHandler(new StringDataSource("bazvalue")));
        return attachments;
    }

    protected void assertAttachmentValueEquals(String expected, Object attachment) throws IOException
    {
        assertTrue(attachment instanceof DataHandler);
        DataHandler dataHandler = (DataHandler) attachment;
        String attachmentString = attachmentToString(dataHandler);
        assertEquals(expected, attachmentString);
    }

    protected String attachmentToString(DataHandler dataHandler) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        dataHandler.writeTo(baos);
        return baos.toString();
    }
}
