/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import org.mule.extension.email.api.EmailBody;
import org.mule.extension.email.api.EmailBuilder;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.internal.MessageBuilder;
import org.mule.extension.email.internal.sender.SMTPConfiguration;
import org.mule.extension.email.internal.sender.SenderConnection;

import java.util.Calendar;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;

/**
 * Represents the send operation.
 *
 * @since 4.0
 */
public final class SendCommand {

  /**
   * Send an email message. The message will be sent to all recipient {@code toAddresses}, {@code ccAddresses},
   * {@code bccAddresses} specified in the {@code emailBuilder}.
   *
   * @param connection    the connection associated to the operation.
   * @param configuration the specified configuration to send the email.
   * @param emailBuilder       the email emailBuilder used to create the email that is going to be sent.
   */
  public void send(SenderConnection connection, SMTPConfiguration configuration, EmailBuilder emailBuilder) {
    try {
      EmailBody body = emailBuilder.getBody();

      Message message = MessageBuilder.newMessage(connection.getSession())
          .withSentDate(Calendar.getInstance().getTime())
          .fromAddresses(isNotBlank(emailBuilder.getFromAddress()) ? emailBuilder.getFromAddress() : configuration.getFrom())
          .to(emailBuilder.getToAddresses())
          .cc(emailBuilder.getCcAddresses())
          .bcc(emailBuilder.getBccAddresses())
          .withSubject(emailBuilder.getSubject())
          .withAttachments(emailBuilder.getAttachments())
          .withBody(body.getContent(), body.getContentType(),
                    body.getEncoding() == null ? configuration.getDefaultEncoding() : body.getEncoding())
          .withHeaders(emailBuilder.getCustomHeaders())
          .build();

      Transport.send(message);
    } catch (MessagingException e) {
      throw new EmailException("Error while sending email: " + e.getMessage(), e);
    }
  }
}
