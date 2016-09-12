/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static javax.mail.Folder.READ_ONLY;
import static org.mule.extension.email.api.EmailAttributesBuilder.fromMessage;
import static org.mule.extension.email.api.EmailContentProcessor.process;
import static org.mule.runtime.core.message.DefaultMultiPartPayload.BODY_ATTRIBUTES;

import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.api.EmailContentProcessor;
import org.mule.extension.email.api.exception.EmailRetrieverException;
import org.mule.extension.email.internal.retriever.RetrieverConnection;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.message.DefaultMultiPartPayload;

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
   * A new {@link Message} is created for each fetched email from the folder, where the payload is the text body of the email and
   * the other metadata is carried by an {@link EmailAttributes} instance.
   * <p>
   * For folder implementations (like IMAP) that support fetching without reading the content, if the content should NOT be read
   * ({@code readContent} = false) the SEEN flag is not going to be set.
   *
   * @param connection the associated {@link RetrieverConnection}.
   * @param folderName the name of the folder where the emails are stored.
   * @param readContent if should read the email content or not.
   * @param matcher a {@link Predicate} of {@link EmailAttributes} used to filter the output list @return a {@link List} of
   *        {@link InternalMessage} carrying all the emails and it's corresponding attributes.
   */
  public List<Message> list(RetrieverConnection connection,
                            String folderName,
                            boolean readContent,
                            Predicate<EmailAttributes> matcher) {
    try {
      Folder folder = connection.getFolder(folderName, READ_ONLY);
      List<Message> list = new LinkedList<>();
      for (javax.mail.Message m : folder.getMessages()) {
        Object body = "";
        EmailAttributes attributes = fromMessage(m);
        if (matcher.test(attributes)) {
          if (readContent) {
            body = EmailContentProcessor.process(m).getBody();

            final List<Message> attachmentParts = process(m).getAttachments();
            if (!attachmentParts.isEmpty()) {
              final List<Message> parts = new ArrayList<>();
              parts.add(Message.builder().payload(body).attributes(BODY_ATTRIBUTES).build());
              parts.addAll(attachmentParts);

              body = new DefaultMultiPartPayload(parts);
            }
          }
          attributes = fromMessage(m);
          list.add(Message.builder().payload(body).attributes(attributes).build());
        }
      }
      return list;
    } catch (MessagingException me) {
      throw new EmailRetrieverException(me);
    }
  }
}
