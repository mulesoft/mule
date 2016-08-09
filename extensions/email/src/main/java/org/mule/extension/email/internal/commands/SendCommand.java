/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import org.mule.extension.email.api.EmailAttachment;
import org.mule.extension.email.api.EmailContent;
import org.mule.extension.email.api.MessageBuilder;
import org.mule.extension.email.api.exception.EmailSenderException;
import org.mule.extension.email.internal.sender.SenderConnection;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Represents the send operation.
 *
 * @since 4.0
 */
public final class SendCommand {

  /**
   * Send an email message. The message will be sent to all recipient {@code toAddresses}, {@code ccAddresses},
   * {@code bccAddresses} specified in the message.
   *
   * @param connection the connection associated to the operation.
   * @param content the text content of the email.
   * @param subject the subject of the email.
   * @param toAddresses the "to" (primary) addresses to deliver the email.
   * @param fromAddress the person(s) that are sending the email.
   * @param defaultCharset the default charset of the email message to be used if the {@param content} don't specify it.
   * @param ccAddresses the carbon copy addresses to deliver the email.
   * @param bccAddresses the blind carbon copy addresses to deliver the email.
   * @param headers a map of custom headers that are bounded with the email.
   * @param attachments the attachments that are bounded in the content of the email.
   */
  public void send(SenderConnection connection, EmailContent content, String subject, List<String> toAddresses,
                   String fromAddress, String defaultCharset, List<String> ccAddresses, List<String> bccAddresses,
                   Map<String, String> headers, List<EmailAttachment> attachments) {
    try {
      Message message = MessageBuilder.newMessage(connection.getSession()).withSentDate(Calendar.getInstance().getTime())
          .fromAddresses(fromAddress).to(toAddresses).cc(ccAddresses).bcc(bccAddresses).withSubject(subject)
          .withAttachments(attachments != null ? attachments : new ArrayList<>())
          .withContent(content.getBody(), content.getContentType(),
                       content.getCharset() == null ? defaultCharset : content.getCharset())
          .withHeaders(headers).build();

      Transport.send(message);
    } catch (MessagingException e) {
      throw new EmailSenderException(e);
    }
  }
}
