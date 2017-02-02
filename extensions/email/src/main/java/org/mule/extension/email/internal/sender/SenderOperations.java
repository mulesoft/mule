/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.sender;


import org.mule.extension.email.api.EmailBuilder;
import org.mule.extension.email.api.exception.EmailSenderErrorTypeProvider;
import org.mule.extension.email.internal.commands.SendCommand;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

/**
 * Basic set of operations which perform send email operations over the SMTP or SMTPS protocol.
 *
 * @since 4.0
 */
public class SenderOperations {

  private final SendCommand sendOperation = new SendCommand();

  /**
   * Sends an email message. The message will be sent to all recipient {@code toAddresses}, {@code ccAddresses},
   * {@code bccAddresses} specified in the message.
   * <p>
   * The content of the message aims to be some type of text (text/plan, text/html) and its composed by the body and it's content
   * type. If no content is specified then the incoming payload it's going to be converted into plain text if possible.
   *
   * @param connection    Connection to use to send the message
   * @param configuration Configuration of the connector
   * @param emailBuilder  The builder of the email that is going to be send.
   */
  @Summary("Sends an email message")
  @Throws(EmailSenderErrorTypeProvider.class)
  public void send(@Connection SenderConnection connection,
                   @UseConfig SMTPConfiguration configuration,
                   EmailBuilder emailBuilder) {
    sendOperation.send(connection, configuration, emailBuilder);
  }
}
