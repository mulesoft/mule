/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.sender;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import org.mule.extension.email.api.Email;
import org.mule.extension.email.api.EmailAttachment;
import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.api.EmailContent;
import org.mule.extension.email.api.exception.EmailSenderException;
import org.mule.extension.email.internal.commands.ForwardCommand;
import org.mule.extension.email.internal.commands.ReplyCommand;
import org.mule.extension.email.internal.commands.SendCommand;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.List;
import java.util.Map;

/**
 * Basic set of operations which perform send email operations over the SMTP or SMTPS protocol.
 *
 * @since 4.0
 */
public class SenderOperations
{

    // TODO: REMOVE WHEN THERE IS DEFAULT PAYLOAD IN THE OPTIONAL ANNOTATION - MULE-9918
    private static final String PAYLOAD = "#[payload]";
    private final SendCommand sendOperation = new SendCommand();
    private final ForwardCommand forwardCommand = new ForwardCommand();
    private final ReplyCommand replyOperation = new ReplyCommand();

    /**
     * Sends an email message. The message will be sent to all recipient {@code toAddresses}, {@code ccAddresses},
     * {@code bccAddresses} specified in the message.
     * <p>
     * The content of the message aims to be some type of text (text/plan, text/html)
     * and its composed by the body and it's content type. If no content is
     * specified then the operation will fail
     *
     * @param connection    Connection to use to send the message
     * @param configuration Configuration of the connector
     * @param content       Content of the message
     * @param subject       Subject of the email message to send. If not set, "[No Subject]" will be used
     * @param toAddresses   List of "To" (primary) email message recipients
     * @param ccAddresses   List of "Cc" (carbon copy) email message recipients
     * @param bccAddresses  List of "Bcc" (blind carbon copy) email message recipients
     * @param headers       Map of custom headers that are bounded with the email message
     * @param attachments   Attachments that are bounded with the email message
     */
    @Summary("Sends an email message")
    public void send(@Connection SenderConnection connection,
                     @UseConfig SMTPConfiguration configuration,
                     @Optional(defaultValue = PAYLOAD) EmailContent content, // TODO: create a transformer from string to EmailContent when the sdk have support for it - MULE-9181.
                     @Optional(defaultValue = "[No Subject]") String subject,
                     List<String> toAddresses,
                     @Optional List<String> ccAddresses,
                     @Optional List<String> bccAddresses,
                     @DisplayName("Additional Headers") @Optional Map<String, String> headers,
                     @Optional List<EmailAttachment> attachments)
    {
        if (content == null)
        {
            throw new EmailSenderException(SendCommand.NO_EMAIL_FOUND);
        }

        sendOperation.send(connection, content, subject, toAddresses,
                           configuration.getFrom(), configuration.getDefaultCharset(),
                           ccAddresses != null ? ccAddresses : emptyList(),
                           bccAddresses != null ? bccAddresses : emptyList(),
                           headers != null ? headers : emptyMap(),
                           attachments);
    }

    /**
     * Forwards an email message. The message will be sent to all recipient addresses.
     * <p>
     * This operation expects an incoming email to take the content in order to forward it,
     * if no email message is found this operation will fail.
     *
     * @param connection    the connection used to send the message.
     * @param configuration the configuration of the connector.
     * @param email         the incoming {@link Email}.
     * @param content       the content of the message to be forwarded
     * @param subject       the subject of the message.
     * @param toAddresses   the "To" (primary) recipients.
     * @param ccAddresses   the "Cc" (carbon copy) recipients.
     * @param bccAddresses  the "Bcc" (blind carbon copy) recipients.
     * @param headers       custom headers of the message.
     */
    @Summary("Forwards an email message")
    public void forward(@Connection SenderConnection connection,
                        @UseConfig SMTPConfiguration configuration,
                        @Optional(defaultValue = PAYLOAD) Email email,
                        @Optional @DisplayName("Email Content") EmailContent content,
                        @Optional String subject,
                        List<String> toAddresses,
                        @Optional List<String> ccAddresses,
                        @Optional List<String> bccAddresses,
                        @DisplayName("Additional Headers") @Optional Map<String, String> headers)
    {
        if (email == null)
        {
            throw new EmailSenderException(ForwardCommand.NO_EMAIL_FOUND);
        }

        forwardCommand.forward(connection, email, content, subject,
                               configuration.getFrom(), configuration.getDefaultCharset(),
                               toAddresses, ccAddresses, bccAddresses, headers);
    }


    /**
     * Replies an email message. The message will be sent to the addresses
     * associated to the replyTo attribute in the {@link EmailAttributes} of
     * the incoming {@code Email}.
     * <p>
     * This operation expects an email in the incoming {@code muleMessage} to reply to, if no email message is found
     * this operation will fail.
     *
     * @param connection    the connection used to send the message.
     * @param configuration the configuration of the connector.
     * @param email         the incoming {@link Email}.
     * @param content       the content of the reply message
     * @param subject       the subject of the message, if none {@code "[No Subject]"} is the default value
     * @param replyToAll    if this reply should be sent to all recipients of this message
     * @param headers       custom headers of the message.
     */
    @Summary("Replies an email message")
    public void reply(@Connection SenderConnection connection,
                      @UseConfig SMTPConfiguration configuration,
                      @Optional(defaultValue = PAYLOAD) Email email,
                      @DisplayName("Email Content") EmailContent content,
                      @Optional String subject,
                      @Optional @DisplayName("Additional Headers") Map<String, String> headers,
                      @Optional(defaultValue = "false") Boolean replyToAll)
    {
        if (email == null)
        {
            throw new EmailSenderException(ReplyCommand.NO_EMAIL_FOUND);
        }
        replyOperation.reply(connection, email, content, subject,
                             configuration.getFrom(), configuration.getDefaultCharset(),
                             headers, replyToAll);
    }

}
