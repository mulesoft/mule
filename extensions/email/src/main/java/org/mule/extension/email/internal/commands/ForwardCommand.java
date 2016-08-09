/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static java.lang.String.format;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.getAttributesFromMessage;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.mapToEmailAttachments;

import org.mule.extension.email.api.EmailAttachment;
import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.api.EmailContent;
import org.mule.extension.email.api.exception.EmailSenderException;
import org.mule.extension.email.internal.sender.SenderConnection;
import org.mule.runtime.api.message.MuleMessage;

import java.util.List;
import java.util.Map;

/**
 * Represents the forward operation.
 *
 * @since 4.0
 */
public final class ForwardCommand {

  private final SendCommand sendCommand = new SendCommand();

  /**
   * Forwards an email message. The message will be sent to all recipient {@code toAddresses}.
   * <p>
   * The forwarded content is taken from the incoming {@link MuleMessage}'s payload. If not possible this operation is going to
   * fail.
   *
   * @param connection the connection associated to the operation.
   * @param muleMessage the incoming {@link MuleMessage} from which the email is going to getPropertiesInstance the content.
   * @param content the content of the email.
   * @param subject the subject of the email.
   * @param from the person who sends the email.
   * @param defaultCharset the default charset of the email message to be used if the {@param content} don't specify it.
   * @param toAddresses the primary recipient addresses of the email.
   * @param ccAddresses the carbon copy recipient addresses of the email.
   * @param bccAddresses the blind carbon copy recipient addresses of the email.
   */
  public void forward(SenderConnection connection, MuleMessage muleMessage, EmailContent content, String subject, String from,
                      String defaultCharset, List<String> toAddresses, List<String> ccAddresses, List<String> bccAddresses,
                      Map<String, String> headers) {

    EmailAttributes attributes = getAttributesFromMessage(muleMessage)
        .orElseThrow(() -> new EmailSenderException("Cannot perform the forward operation if no email is provided."));

    if (subject == null) {
      subject = "Fwd: " + attributes.getSubject();
    }

    String body = muleMessage.getPayload().toString();
    String forwardBody = content != null ? format("%s\r\n\r\n%s", content.getBody(), body) : body;
    EmailContent forwardContent = content != null ? new EmailContent(forwardBody, content.getContentType(), content.getCharset())
        : new EmailContent(forwardBody, defaultCharset);
    List<EmailAttachment> emailAttachments = mapToEmailAttachments(muleMessage.getPayload());
    sendCommand.send(connection, forwardContent, subject, toAddresses, from, defaultCharset, ccAddresses, bccAddresses, headers,
                     emailAttachments);

  }
}
