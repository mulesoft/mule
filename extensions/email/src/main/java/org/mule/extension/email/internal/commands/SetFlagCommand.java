/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static java.lang.String.format;
import static javax.mail.Folder.READ_WRITE;
import org.mule.extension.email.api.exception.EmailNotFoundException;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.internal.mailbox.MailboxConnection;

import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;

/**
 * Represents an operation that sets flags in email messages.
 * <p>
 * This operation works only for emails retrieved from {@link UIDFolder}, this means that
 * email messages <strong>must</strong> contain an UID (unique identifier) in the specified folder.
 * <p>
 * The POP3 protocol does not have support for retrieving an email from it's UID, on the other side
 * the IMAP protocol is fully capable of doing the described action.
 *
 * @since 4.0
 */
public class SetFlagCommand {

  /**
   * Sets the specified {@code flag} into the email of UID (unique identifier) {@code emailId}.
   * <p>
   * This method only works for {@link UIDFolder}s, that are handled by the IMAP protocol
   *
   * @param connection the associated {@link MailboxConnection}.
   * @param folderName the name of the folder where the emails are stored.
   * @param flag       the {@link Flag} that wants to be set in the email message.
   * @param emailId    the unique identifier of the email in the corresponding {@link UIDFolder} of name {@code folderName}
   */
  public void setByUID(MailboxConnection connection, String folderName, Flag flag, long emailId) {
    try {
      UIDFolder folder = (UIDFolder) connection.getFolder(folderName, READ_WRITE);
      javax.mail.Message message = folder.getMessageByUID(emailId);
      if (message == null) {
        throw new EmailNotFoundException(format("No email was found with id:[%s]", emailId));
      }
      message.setFlag(flag, true);
    } catch (MessagingException e) {
      throw new EmailException(format("Error while setting [%s] flag in email of id [%s]", flag.toString(), emailId), e);
    }
  }

  /**
   * Sets the specified {@code flag} to the email of mailbox number {@code number}
   * <p>
   * This method only works for {@link UIDFolder}s, that are handled by the IMAP protocol
   *
   * @param connection the associated {@link MailboxConnection}.
   * @param folderName the name of the folder where the emails are stored.
   * @param flag       the {@link Flag} that wants to be set in the email message.
   * @param number     the number of the email in the corresponding {@link Folder} of name {@code folderName}
   */
  public void setByNumber(MailboxConnection connection, String folderName, Flag flag, int number) {
    try {
      Folder folder = connection.getFolder(folderName, READ_WRITE);
      javax.mail.Message message = folder.getMessage(number);
      if (message == null) {
        throw new EmailNotFoundException(format("No email was found in the mailbox of number:[%s]", number));
      }
      message.setFlag(flag, true);
    } catch (MessagingException e) {
      throw new EmailException(format("Error while setting [%s] flag in email number:[%s]", flag.toString(), number), e);
    }
  }
}
