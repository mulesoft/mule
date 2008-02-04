/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformer.simple.ByteArrayToObject;
import org.mule.transformer.simple.ObjectToByteArray;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;

public class MuleMessageTestCase extends AbstractMuleTestCase
{

    public void testAttachmentPersistence() throws Exception
    {
        ObjectToByteArray transformer = new ObjectToByteArray();
        transformer.setAcceptUMOMessage(true);

        MuleEvent event = RequestContext.setEvent(getTestEvent("Mmm... attachments!"));
        MuleMessage msg = event.getMessage();
        msg.addAttachment("test-attachment", new DataHandler(new StringDataSource("attachment")));

        Object serialized = transformer.transform(msg);
        assertNotNull(serialized);

        MuleMessage deserialized = (MuleMessage) new ByteArrayToObject().transform(serialized);
        assertNotNull(deserialized);
        assertEquals(deserialized.getUniqueId(), msg.getUniqueId());
        assertEquals(deserialized.getPayload(), msg.getPayload());
        assertEquals(deserialized.getAttachmentNames(), msg.getAttachmentNames());
    }

    // silly little fake DataSource so that we don't need to use javamail
    protected static class StringDataSource implements DataSource
    {
        protected String content;

        public StringDataSource(String payload)
        {
            super();
            content = payload;
        }

        public InputStream getInputStream() throws IOException
        {
            return new ByteArrayInputStream(content.getBytes());
        }

        public OutputStream getOutputStream()
        {
            throw new UnsupportedOperationException("Read-only javax.activation.DataSource");
        }

        public String getContentType()
        {
            return "text/plain";
        }

        public String getName()
        {
            return "StringDataSource";
        }
    }

}
