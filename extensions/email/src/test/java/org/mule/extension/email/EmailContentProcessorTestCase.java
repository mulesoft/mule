/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.email.internal.EmailContentProcessor.process;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_JSON_ATTACHMENT_NAME;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_TEXT_PLAIN_ATTACHMENT_NAME;
import static org.mule.extension.email.util.EmailTestUtils.assertAttachmentContent;
import static org.mule.extension.email.util.EmailTestUtils.getSinglePartTestMessage;
import static org.mule.extension.email.util.EmailTestUtils.getMultipartTestMessage;

import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.Message;

import org.junit.Test;

public class EmailContentProcessorTestCase
{
    @Test
    public void emailTextBodyFromMultipart() throws Exception
    {
        Message message = getMultipartTestMessage();
        String messageBody = process(message).getBody();
        assertThat(messageBody, is(EMAIL_CONTENT));
    }

    @Test
    public void emailTextBodyFromSinglePart() throws Exception
    {
        Message message = getSinglePartTestMessage();
        String messageBody = process(message).getBody();
        assertThat(messageBody, is(EMAIL_CONTENT));
    }

    @Test
    public void emailAttachmentsFromMultipart() throws Exception
    {
        Message message = getMultipartTestMessage();
        Map<String, DataHandler> attachments = process(message).getAttachments();
        assertThat(attachments.entrySet(), hasSize(2));
        assertAttachmentContent(attachments, EMAIL_TEXT_PLAIN_ATTACHMENT_NAME, EMAIL_TEXT_PLAIN_ATTACHMENT_CONTENT);
        assertAttachmentContent(attachments, EMAIL_JSON_ATTACHMENT_NAME, EMAIL_JSON_ATTACHMENT_CONTENT);
    }

}
