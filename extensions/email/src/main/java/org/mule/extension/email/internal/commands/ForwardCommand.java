/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static java.lang.String.format;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.mapToEmailAttachments;
import org.mule.extension.email.api.Email;
import org.mule.extension.email.api.EmailAttachment;
import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.api.EmailContent;
import org.mule.extension.email.api.exception.EmailSenderException;
import org.mule.extension.email.internal.sender.SenderConnection;
import org.mule.runtime.api.message.MuleMessage;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.getAttributesFromMessage;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.mapToEmailAttachments;

/**
 * Represents the forward operation.
 *
 * @since 4.0
 */
public final class ForwardCommand
{
    public static final String NO_EMAIL_FOUND = "Cannot perform the forward operation if no email is provided";

    private final SendCommand sendCommand = new SendCommand();

    /**
     * Forwards an email message. The message will be sent to all recipient
     * {@code toAddresses}.
     * <p>
     * The forwarded content is taken from the incoming {@link MuleMessage}'s payload. If not possible
     * this operation is going to fail.
     *
     * @param connection   the connection associated to the operation.
     * @param email        the incoming {@link Email} from which the properties will be obtained.
     * @param content      the content of the email.
     * @param subject      the subject of the email.
     * @param from         the person who sends the email.
     * @param toAddresses  the primary recipient addresses of the email.
     * @param ccAddresses  the carbon copy recipient addresses of the email.
     * @param bccAddresses the blind carbon copy recipient addresses of the email.
     */
    public void forward(SenderConnection connection,
                        Email email,
                        EmailContent content,
                        String subject,
                        String from,
                        String defaultCharset,
                        List<String> toAddresses,
                        List<String> ccAddresses,
                        List<String> bccAddresses,
                        Map<String, String> headers)
    {
        EmailAttributes attributes = email.getAttributes();
        subject = subject == null ? "Fwd: " + attributes.getSubject() : subject;

        String forwardedBody = email.getContent().getBody();
        String newBody = content != null ? format("%s\r\n\r\n%s", content.getBody(), forwardedBody) : forwardedBody;
        EmailContent forwardContent = content != null
                                      ? new EmailContent(newBody, content.getContentType(), content.getCharset())
                                      : new EmailContent(newBody, defaultCharset);
        List<EmailAttachment> emailAttachments = mapToEmailAttachments(attributes.getAttachments());

        sendCommand.send(connection, forwardContent, subject, toAddresses, from, defaultCharset, ccAddresses, bccAddresses, headers, emailAttachments);

    }
}
