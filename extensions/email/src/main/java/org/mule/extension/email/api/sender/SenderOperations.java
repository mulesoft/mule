/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.sender;

import org.mule.extension.email.api.EmailAttachment;
import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.api.EmailContent;
import org.mule.extension.email.internal.commands.ForwardCommand;
import org.mule.extension.email.internal.commands.ReplyCommand;
import org.mule.extension.email.internal.commands.SendCommand;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basic set of operations which perform send email
 * operations over the SMTP or SMTPS protocol.
 *
 * @since 4.0
 */
public class SenderOperations
{

    private final SendCommand sendOperation = new SendCommand();
    private final ForwardCommand forwardCommand = new ForwardCommand();
    private final ReplyCommand replyOperation = new ReplyCommand();

    // TODO: REMOVE WHEN THERE IS DEFAULT PAYLOAD IN THE OPTIONAL ANNOTATION - MULE-9918
    private static final String PAYLOAD = "#[payload]";

    /**
     * Send an email message. The message will be sent to all recipient
     * {@code toAddresses}, {@code ccAddresses}, {@code bccAddresses}
     * specified in the message.
     * <p>
     * The content of the message aims to be some type of text (text/plan, text/html)
     * and its composed by the body and it's content type. If no content is
     * specified then the incoming payload it's going to be converted into plain text if
     * possible.
     *
     * @param connection    the connection used to send the message
     * @param configuration the configuration of the connector
     * @param content       the content of the message
     * @param subject       the subject of the message, if none {@code "[No Subject]"} is the default value
     * @param toAddresses   the "To" (primary) recipients.
     * @param ccAddresses   the "Cc" (carbon copy) recipients.
     * @param bccAddresses  the "Bcc" (blind carbon copy) recipients.
     * @param headers       custom headers of the message.
     * @param attachments   the attachments bounded to the message.
     */
    public void send(@Connection SenderConnection connection,
                     @UseConfig SMTPConfiguration configuration,
                     EmailContent content, // TODO: create a transformer from string to EmailContent when the sdk have support for it - MULE-9181.
                     @Optional(defaultValue = "[No Subject]") String subject,
                     List<String> toAddresses,
                     @Optional List<String> ccAddresses,
                     @Optional List<String> bccAddresses,
                     @Optional Map<String, String> headers,
                     @Optional List<EmailAttachment> attachments)
    {
        sendOperation.send(connection,
                           content,
                           subject,
                           toAddresses,
                           configuration.getFrom(),
                           ccAddresses != null ? ccAddresses : new ArrayList<>(),
                           bccAddresses != null ? bccAddresses : new ArrayList<>(),
                           headers != null ? headers : new HashMap<>(),
                           attachments);
    }

    /**
     * Forwards an email message. The message will be sent to all recipient addresses.
     * <p>
     * This operation expects an email in the incoming {@code muleMessage}
     * to take the content in order forward, if no email message is found this operation will fail.
     *
     * @param connection    the connection used to send the message.
     * @param configuration the configuration of the connector.
     * @param muleMessage   the incoming {@link MuleMessage}.
     * @param content       the content of the message to be forwarded
     * @param subject       the subject of the message.
     * @param toAddresses   the "To" (primary) recipients.
     * @param ccAddresses   the "Cc" (carbon copy) recipients.
     * @param bccAddresses  the "Bcc" (blind carbon copy) recipients.
     * @param headers       custom headers of the message.
     */
    public void forward(@Connection SenderConnection connection,
                        @UseConfig SMTPConfiguration configuration,
                        MuleMessage muleMessage,
                        @Optional EmailContent content,
                        @Optional String subject,
                        List<String> toAddresses,
                        @Optional List<String> ccAddresses,
                        @Optional List<String> bccAddresses,
                        @Optional Map<String, String> headers)
    {
        forwardCommand.forward(connection,
                               muleMessage,
                               content,
                               subject,
                               configuration.getFrom(),
                               toAddresses,
                               ccAddresses,
                               bccAddresses,
                               headers);
    }


    /**
     * Replies an email message. The message will be sent to the addresses
     * associated to the replyTo attribute in the {@link EmailAttributes} of
     * the incoming {@code muleMessage}.
     * <p>
     * This operation expects an email in the incoming {@code muleMessage}
     * to reply to, if no email message is found this operation will fail.
     *
     * @param connection    the connection used to send the message.
     * @param configuration the configuration of the connector.
     * @param muleMessage   the incoming {@link MuleMessage}.
     * @param content       the content of the reply message
     * @param subject       the subject of the message, if none {@code "[No Subject]"} is the default value
     * @param replyToAll    if this reply should be sent to all recipients of this message
     * @param headers       custom headers of the message.
     */
    public void reply(@Connection SenderConnection connection,
                      @UseConfig SMTPConfiguration configuration,
                      MuleMessage muleMessage,
                      EmailContent content,
                      @Optional String subject,
                      @Optional Map<String, String> headers,
                      @Optional(defaultValue = "false") Boolean replyToAll)
    {
        replyOperation.reply(connection,
                             muleMessage,
                             content,
                             subject,
                             configuration.getFrom(),
                             headers,
                             replyToAll);
    }

}
