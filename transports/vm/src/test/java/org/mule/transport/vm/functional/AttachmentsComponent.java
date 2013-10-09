/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.vm.functional;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;

import java.io.File;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

/**
 * A test service that reads inbound attachments and sends an attachment back. This
 * class is only suitable for the VMAttachementsTestCase.
 */
public class AttachmentsComponent implements Callable
{
    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        MuleMessage msg = eventContext.getMessage();
        if (msg.getInboundAttachmentNames().size() == 2)
        {
            throw new IllegalArgumentException("There shuold be 2 attachments");
        }

        DataHandler dh = msg.getInboundAttachment("test-attachment");
        if (dh == null)
        {
            throw new IllegalArgumentException("test-attachment is not on the message");
        }
        if (!dh.getContentType().startsWith("text/xml"))
        {
            throw new IllegalArgumentException("content type is not text/xml");
        }

        if (!"Mmm... attachments!".equals(msg.getPayloadAsString()))
        {
            throw new IllegalArgumentException("payload is incorrect");
        }
        // Lets return an image
        MuleMessage result = new DefaultMuleMessage("here is one for you!", eventContext.getMuleContext());
        FileDataSource ds = new FileDataSource(
            new File("transports/vm/src/test/resources/test.gif").getAbsoluteFile());
        result.addOutboundAttachment("mule", new DataHandler(ds));
        return result;
    }
}
