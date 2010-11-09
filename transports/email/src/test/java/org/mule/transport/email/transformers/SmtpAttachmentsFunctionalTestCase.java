/*
 * $Id:  $
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Created by IntelliJ IDEA.
 * User: mike.schilling
 * Date: Oct 15, 2010
 * Time: 3:00:34 PM
 * To change this template use File | Settings | File Templates.
 */

package org.mule.transport.email.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transport.email.functional.AbstractEmailFunctionalTestCase;

import java.util.Arrays;
import java.util.List;

import javax.activation.MimeType;
import javax.mail.BodyPart;
import javax.mail.internet.MimeMultipart;

public class SmtpAttachmentsFunctionalTestCase extends AbstractEmailFunctionalTestCase
{

    public SmtpAttachmentsFunctionalTestCase()
    {
        super(STRING_MESSAGE, "smtp", "smtp-functional-test-all-attachments.xml");
        setAddAttachments(true);
    }

    public void testSend() throws Exception
    {
        doSend();
    }

    @Override
    protected void verifyMessage(MimeMultipart content) throws Exception
    {
        assertEquals(4, content.getCount());
        verifyMessage((String) content.getBodyPart(0).getContent());
        List expectedTypes = Arrays.asList("text/plain", "application/xml", "application/text");
        for (int i = 1; i < 4; i++)
        {
            BodyPart part = content.getBodyPart(i);
            String type = part.getContentType();
            MimeType mt = new MimeType(type);
            assertTrue(expectedTypes.contains(mt.getPrimaryType() + "/" + mt.getSubType()));
        }
    }

    public static class AddOutboundAttachments extends AbstractMessageTransformer
    {
        @Override
        public Object transformMessage(MuleMessage msg, String outputEncoding) throws TransformerException
        {
            try
            {
                msg.addOutboundAttachment("seeya", "seeya", "application/text");
                msg.addOutboundAttachment("goodbye", "goodbye", "application/xml");
            }
            catch (Exception e)
            {
                throw new TransformerException(this, e);
            }
            return msg.getPayload();
        }
    }
}
