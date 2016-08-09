/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.sender;


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
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basic set of operations which perform send email operations over the SMTP or SMTPS protocol.
 *
 * @since 4.0
 */
public class SenderOperations {

  // TODO: REMOVE WHEN THERE IS DEFAULT PAYLOAD IN THE OPTIONAL ANNOTATION - MULE-9918
  private static final String PAYLOAD = "#[payload]";
  private final SendCommand sendOperation = new SendCommand();
  private final ForwardCommand forwardCommand = new ForwardCommand();
  private final ReplyCommand replyOperation = new ReplyCommand();

  /**
   * Sends an email message. The message will be sent to all recipient {@code toAddresses}, {@code ccAddresses},
   * {@code bccAddresses} specified in the message.
   * <p>
   * The content of the message aims to be some type of text (text/plan, text/html) and its composed by the body and it's content
   * type. If no content is specified then the incoming payload it's going to be converted into plain text if possible.
   *
   * @param connection Connection to use to send the message
   * @param configuration Configuration of the connector
   * @param content Content of the message
   * @param subject Subject of the email message to send. If not set, "[No Subject]" will be used
   * @param toAddresses List of "To" (primary) email message recipients
   * @param ccAddresses List of "Cc" (carbon copy) email message recipients
   * @param bccAddresses List of "Bcc" (blind carbon copy) email message recipients
   * @param headers Map of custom headers that are bounded with the email message
   * @param attachments Attachments that are bounded with the email message
   */
  @Summary("Sends an email message")
  public void send(@Connection SenderConnection connection, @UseConfig SMTPConfiguration configuration, EmailContent content, // TODO:
                   // create
                   // a
                   // transformer
                   // from
                   // string
                   // to
                   // EmailContent
                   // when
                   // the
                   // sdk
                   // have
                   // support
                   // for
                   // it
                   // -
                   // MULE-9181.
                   @Optional(defaultValue = "[No Subject]") String subject, List<String> toAddresses,
                   @Optional List<String> ccAddresses, @Optional List<String> bccAddresses,
                   @DisplayName("Additional Headers") @Optional Map<String, String> headers,
                   @Optional List<EmailAttachment> attachments) {
    sendOperation.send(connection, content, subject, toAddresses, configuration.getFrom(), configuration.getDefaultCharset(),
                       ccAddresses != null ? ccAddresses : new ArrayList<>(),
                       bccAddresses != null ? bccAddresses : new ArrayList<>(), headers != null ? headers : new HashMap<>(),
                       attachments);
  }

  /**
   * Forwards an email message. The message will be sent to all recipient addresses.
   * <p>
   * This operation expects an email in the incoming {@code muleMessage} to take the content in order forward, if no email message
   * is found this operation will fail.
   *
   * @param connection Connection to use to forward the message.
   * @param configuration Configuration of the connector.
   * @param muleMessage The incoming {@link MuleMessage}.
   * @param content Content of the message to be forwarded
   * @param subject Subject of the email message to forward. If not set, the subject of the forwarded message will be used
   * @param toAddresses List of "To" (primary) email message recipients
   * @param ccAddresses List of "Cc" (carbon copy) email message recipients
   * @param bccAddresses List of "Bcc" (blind carbon copy) email message recipients
   * @param headers Map of custom headers that are bounded with the email message
   */
  @Summary("Forwards an email message")
  public void forward(@Connection SenderConnection connection, @UseConfig SMTPConfiguration configuration,
                      MuleMessage muleMessage, @Optional @DisplayName("Email Content") EmailContent content,
                      @Optional String subject, List<String> toAddresses, @Optional List<String> ccAddresses,
                      @Optional List<String> bccAddresses,
                      @DisplayName("Additional Headers") @Optional Map<String, String> headers) {
    forwardCommand.forward(connection, muleMessage, content, subject, configuration.getFrom(), configuration.getDefaultCharset(),
                           toAddresses, ccAddresses, bccAddresses, headers);
  }

  /**
   * Replies an email message. The message will be sent to the addresses associated to the replyTo attribute in the
   * {@link EmailAttributes} of the incoming {@code muleMessage}.
   * <p>
   * This operation expects an email in the incoming {@code muleMessage} to reply to, if no email message is found this operation
   * will fail.
   *
   * @param connection Connection to use to reply the message.
   * @param configuration Configuration of the connector.
   * @param muleMessage The incoming {@link MuleMessage}.
   * @param content Content of the reply message
   * @param subject Subject of the email message, if not set, the subject of the replied email message will be used
   * @param headers Map of custom headers that are bounded with the email message
   * @param replyToAll Whether this reply should be sent to all recipients of this message
   */
  @Summary("Replies an email message")
  public void reply(@Connection SenderConnection connection, @UseConfig SMTPConfiguration configuration, MuleMessage muleMessage,
                    @DisplayName("Email Content") EmailContent content, @Optional String subject,
                    @Optional @DisplayName("Additional Headers") Map<String, String> headers,
                    @Optional(defaultValue = "false") Boolean replyToAll) {
    replyOperation.reply(connection, muleMessage, content, subject, configuration.getFrom(), configuration.getDefaultCharset(),
                         headers, replyToAll);
  }
}
