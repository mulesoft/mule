/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static java.lang.String.format;
import static javax.mail.Folder.READ_WRITE;

import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.internal.manager.MailboxConnection;
import org.mule.runtime.api.message.Message;

import java.util.List;

import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.MessagingException;

/**
 * Represents the set flag operation. Sets on of the {@link Flag}s on an email message.
 *
 * @since 4.0
 */
public class SetFlagCommand {

  private final EmailIdConsumerExecutor executor = new EmailIdConsumerExecutor();

  private static final String SET_FLAG_ERROR_MESSAGE_MASK = "Error while setting [%s] flag in email of id [%s]";

  /**
   * Sets the specified {@code flag} into the email of number {@code emailId}
   * <p>
   * if no emailId is specified, the operation will try to find an email or {@link List} of emails in the incoming
   * {@link Message}.
   * <p>
   * If no email(s) are found in the {@link Message} and no {@code emailId} is specified. the operation will fail.
   *
   * @param muleMessage the incoming {@link Message}.
   * @param connection the associated {@link MailboxConnection}.
   * @param folderName the name of the folder where the email(s) is going to be fetched.
   * @param emailId the optional number of the email to be marked. for default the email is taken from the incoming
   *        {@link Message}.
   * @param flag the flag to be set.
   */
  public void set(Message muleMessage, MailboxConnection connection, String folderName, Integer emailId, Flag flag) {
    Folder folder = connection.getFolder(folderName, READ_WRITE);
    executor.execute(muleMessage, emailId, id -> {
      try {
        javax.mail.Message message = folder.getMessage(id);
        message.setFlag(flag, true);
      } catch (MessagingException e) {
        throw new EmailException(format(SET_FLAG_ERROR_MESSAGE_MASK, flag.toString(), id), e);
      }
    });
  }
}
