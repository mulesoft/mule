/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.getAttributesFromMessage;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.mapToEmailAttachments;
import org.mule.extension.email.api.EmailAttachment;
import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.api.EmailContent;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.internal.sender.SenderConnection;
import org.mule.runtime.api.message.MuleMessage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

/**
 * Represents the reply operation.
 *
 * @since 4.0
 */
public final class ReplyCommand {

  public static final String IN_REPLY_TO_HEADER = "In-Reply-To";
  public static final String NO_EMAIL_FOUND = "Cannot perform the reply operation if no email is provided";

  private final SendCommand sendCommand = new SendCommand();

  /**
   * Replies an email message. The message will be sent to the addresses associated to the replyTo attribute in the
   * {@link EmailAttributes} of the incoming {@code muleMessage}.
   * <p>
   * If no email message is found in the incoming {@link MuleMessage} this operation will fail.
   *
   * @param connection     the connection associated to the operation
   * @param muleMessage    the incoming {@link MuleMessage} from which the email is going to getPropertiesInstance the content.
   * @param content        the content of the reply message.
   * @param subject        the subject of the email. is none is set then one will be created using the subject from the replying email.
   * @param from           the person who sends the email.
   * @param defaultCharset the default charset of the email message to be used if the {@param content} don't specify it.
   * @param headers        a custom set of headers.
   * @param attachments    Attachments that are bounded with the email message
   * @param replyToAll     if this reply should be sent to all recipients of this message, or only the sender of the received email.
   */
  public void reply(SenderConnection connection,
                    MuleMessage muleMessage,
                    EmailContent content,
                    String subject,
                    String from,
                    String defaultCharset,
                    Map<String, String> headers,
                    List<EmailAttachment> attachments,
                    Boolean replyToAll) {
    EmailAttributes attributes = getAttributesFromMessage(muleMessage).orElseThrow(() -> new EmailException(NO_EMAIL_FOUND));

    List<String> replyTo = attributes.getReplyToAddresses();
    if (isEmpty(replyTo)) {
      replyTo = attributes.getToAddresses();
    }

    if (subject == null) {
      subject = "Re: " + attributes.getSubject();
    }

    ImmutableMap.Builder<String, String> replyHeaders = ImmutableMap.builder();
    replyHeaders.putAll(headers)
        .putAll(attributes.getHeaders())
        .put(IN_REPLY_TO_HEADER, Integer.toString(attributes.getId()));

    List<String> ccAddresses = replyToAll ? attributes.getCcAddresses() : emptyList();

    ImmutableList<EmailAttachment> emailAttachments =
        ImmutableList.<EmailAttachment>builder()
            .addAll(mapToEmailAttachments(muleMessage.getPayload()))
            .addAll(attachments)
            .build();

    sendCommand.send(connection,
                     content,
                     subject,
                     replyTo,
                     from,
                     defaultCharset,
                     ccAddresses,
                     emptyList(),
                     replyHeaders.build(),
                     emailAttachments);
  }
}
