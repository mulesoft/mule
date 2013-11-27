/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.transformers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.transport.email.functional.AbstractEmailFunctionalTestCase;

import java.util.Arrays;
import java.util.List;

import javax.activation.MimeType;
import javax.mail.BodyPart;
import javax.mail.internet.MimeMultipart;

import org.junit.Test;

public class SmtpAttachmentsFunctionalInboundAttachmentsOnlyTestCase extends AbstractEmailFunctionalTestCase
{

    public SmtpAttachmentsFunctionalInboundAttachmentsOnlyTestCase()
    {
        super(STRING_MESSAGE, "smtp");
        setAddAttachments(true);
    }

    @Override
    protected String getConfigFile()
    {
        return "smtp-functional-test-inbound-attachments-only-flow.xml";
    }

    @Test
    public void testSend() throws Exception
    {
        doSend();
    }

    @Override
    protected void verifyMessage(MimeMultipart content) throws Exception
    {
        assertEquals(3, content.getCount());
        verifyMessage((String) content.getBodyPart(0).getContent());
        List<String> expectedTypes = Arrays.asList("text/plain", "text/xml");
        for (int i = 0; i < 3; i++)
        {
            BodyPart part = content.getBodyPart(i);
            String type = part.getContentType();
            MimeType mt = new MimeType(type);
            assertTrue(expectedTypes.contains(mt.getPrimaryType() + "/" + mt.getSubType()));
        }
    }
}
