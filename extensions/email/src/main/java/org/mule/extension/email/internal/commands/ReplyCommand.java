/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.mapToEmailAttachments;
import org.mule.extension.email.api.Email;
import org.mule.extension.email.api.EmailAttachment;
import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.api.EmailContent;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.internal.sender.SenderConnection;
import org.mule.runtime.api.message.MuleMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.getAttributesFromMessage;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.mapToEmailAttachments;

/**
 * Represents the reply operation.
 *
 * @since 4.0
 */
public final class ReplyCommand
{

    public static final String IN_REPLY_TO_HEADER = "In-Reply-To";
    public static final String NO_EMAIL_FOUND = "Cannot perform the reply operation if no email is provided";

    private final SendCommand sendCommand = new SendCommand();

    /**
     * Replies an email message. The message will be sent to the addresses
     * associated to the replyTo attribute in the {@link EmailAttributes} of
     * the incoming {@code muleMessage}.
     * <p>
     * If no email message is found in the incoming {@link MuleMessage} this operation will fail.
     *
     * @param connection   the connection associated to the operation
     * @param repliedEmail the incoming {@link Email} from which the properties for replying will be obtained.
     * @param replyContent the content of the reply email.
     * @param subject      the subject of the email. is none is set then one will be created using the subject from the replying email.
     * @param from         the person who sends the email.
     * @param headers      a custom set of headers.
     * @param replyToAll   if this reply should be sent to all recipients of this message, or only the sender of the received email.
     */
    public void reply(SenderConnection connection,
                      Email repliedEmail,
                      EmailContent replyContent,
                      String subject,
                      String from,
                      String defaultCharset,
                      Map<String, String> headers,
                      Boolean replyToAll)
    {
        EmailAttributes attributes = repliedEmail.getAttributes();
        List<String> replyTo = attributes.getReplyToAddresses();
        if (isEmpty(replyTo))
        {
            replyTo = attributes.getToAddresses();
        }

        subject = subject == null ? "Re: " + attributes.getSubject() : subject;
        headers = headers == null ? new HashMap<>() : headers;

        headers.put(IN_REPLY_TO_HEADER, Integer.toString(attributes.getId()));
        headers.putAll(attributes.getHeaders());
        List<String> ccAddresses = replyToAll ? attributes.getCcAddresses() : new ArrayList<>();
        List<EmailAttachment> emailAttachments = mapToEmailAttachments(attributes.getAttachments());

        sendCommand.send(connection, replyContent, subject, replyTo, from, defaultCharset, ccAddresses, emptyList(), headers, emailAttachments);
    }
}
