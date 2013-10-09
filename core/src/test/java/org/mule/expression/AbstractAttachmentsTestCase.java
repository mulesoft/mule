/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
