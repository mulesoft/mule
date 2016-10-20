/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static javax.mail.Folder.READ_ONLY;
import static org.mule.runtime.core.message.DefaultMultiPartPayload.BODY_ATTRIBUTES;
import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.api.predicate.BaseEmailPredicateBuilder;
import org.mule.extension.email.internal.mailbox.MailboxAccessConfiguration;
import org.mule.extension.email.internal.mailbox.MailboxConnection;
import org.mule.extension.email.internal.util.EmailContentProcessor;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.runtime.extension.api.runtime.operation.OperationResult;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import javax.mail.Folder;
import javax.mail.MessagingException;


/**
 * Represents the list emails operation.
 *
 * @since 4.0
 */
public final class ListCommand {

  /**
   * Retrieves all the emails in the specified {@code folderName}.
   * <p>
   * A new {@link OperationResult} is created for each fetched email from the folder, where the payload is the text body of the email
   * and the other metadata is carried by an {@link BaseEmailAttributes} instance.
   * <p>
   * For folder implementations (like IMAP) that support fetching without reading the content, if the content should NOT be read
   * ({@code shouldReadContent} = false) the SEEN flag is not going to be set.
   *
   * @param configuration       The {@link MailboxAccessConfiguration} associated to this operation.
   * @param connection          the associated {@link MailboxConnection}.
   * @param folderName          the name of the folder where the emails are stored.
   * @param matcherBuilder      a {@link Predicate} of {@link BaseEmailAttributes} used to filter the output list @return a
   *                            {@link List} of {@link Message} carrying all the emails and it's corresponding attributes.
   */
  public <T extends BaseEmailAttributes> List<OperationResult<Object, T>> list(MailboxAccessConfiguration configuration,
                                                                               MailboxConnection connection,
                                                                               String folderName,
                                                                               BaseEmailPredicateBuilder matcherBuilder) {
    Predicate<BaseEmailAttributes> matcher = matcherBuilder != null ? matcherBuilder.build() : e -> true;
    try {
      Folder folder = connection.getFolder(folderName, READ_ONLY);
      List<OperationResult<Object, T>> retrievedEmails = new LinkedList<>();
      for (javax.mail.Message m : folder.getMessages()) {
        Object emailContent = "";
        T attributes = configuration.parseAttributesFromMessage(m, folder);
        if (matcher.test(attributes)) {
          if (configuration.isEagerlyFetchContent()) {
            emailContent = readContent(m);
            // Attributes are parsed again since they may change after the email has been read.
            attributes = configuration.parseAttributesFromMessage(m, folder);
          }
          OperationResult<Object, T> operationResult = OperationResult.<Object, T>builder()
              .output(emailContent)
              .attributes(attributes)
              .build();
          retrievedEmails.add(operationResult);
        }
      }
      return retrievedEmails;
    } catch (MessagingException me) {
      throw new EmailException("Error while retrieving emails: " + me.getMessage(), me);
    }
  }

  private Object readContent(javax.mail.Message m) {
    Object emailContent;
    EmailContentProcessor processor = EmailContentProcessor.getInstance(m);
    String body = processor.getBody();
    List<Message> parts = new ArrayList<>();
    List<Message> attachments = processor.getAttachments();

    if (!attachments.isEmpty()) {
      parts.add(Message.builder().payload(body).attributes(BODY_ATTRIBUTES).build());
      parts.addAll(attachments);
      emailContent = new DefaultMultiPartPayload(parts);
    } else {
      emailContent = body;
    }
    return emailContent;
  }
}
