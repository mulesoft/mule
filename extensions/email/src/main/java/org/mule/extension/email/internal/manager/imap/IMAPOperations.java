/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.manager.imap;

import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Flags.Flag.SEEN;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.INBOX_FOLDER;
import org.mule.extension.email.api.attributes.ImapEmailAttributes;
import org.mule.extension.email.api.predicate.ImapEmailPredicateBuilder;
import org.mule.extension.email.internal.commands.ExpungeCommand;
import org.mule.extension.email.internal.commands.ListCommand;
import org.mule.extension.email.internal.commands.SetFlagCommand;
import org.mule.extension.email.internal.manager.CommonEmailOperations;
import org.mule.extension.email.internal.manager.MailboxAccessConfiguration;
import org.mule.extension.email.internal.manager.MailboxConnection;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.OperationResult;

import java.util.List;

/**
 * Basic set of operations which perform on top the IMAP email protocol.
 *
 * @since 4.0
 */
public class IMAPOperations {

  private final SetFlagCommand setFlagCommand = new SetFlagCommand();
  private final ExpungeCommand expungeCommand = new ExpungeCommand();
  private final ListCommand listCommand = new ListCommand();

  /**
   * List all the emails in the configured imap mailBoxFolder that match with the specified {@code imapMatcher} criteria.
   *
   * @param config        The {@link MailboxAccessConfiguration} associated to this operation.
   * @param connection    The corresponding {@link MailboxConnection} instance.
   * @param mailboxFolder Mailbox folder where the emails are going to be fetched
   * @param imapMatcher   Email Matcher which gives the capability of filter the retrieved emails
   * @return an {@link OperationResult} {@link List} carrying all the emails content
   * and it's corresponding {@link ImapEmailAttributes}.
   */
  @Summary("List all the emails in the given POP3 Mailbox Folder")
  public List<OperationResult<Object, ImapEmailAttributes>> listImap(@UseConfig IMAPConfiguration config,
                                                                     @Connection MailboxConnection connection,
                                                                     @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder,
                                                                     @DisplayName("Matcher") @Optional ImapEmailPredicateBuilder imapMatcher) {
    return listCommand.list(config, connection, mailboxFolder, imapMatcher);
  }

  /**
   * Marks an incoming email as READ.
   * <p>
   * This operation can target a single email, but if no emailID is specified and the incoming {@link Message} is carrying a list
   * of emails this operation will mark all the emails that the {@link Message} is carrying if they belong to the specified
   * folder.
   *
   * @param message       The incoming {@link Message}.
   * @param connection    The corresponding {@link MailboxConnection} instance.
   * @param mailboxFolder Folder where the emails are going to be marked as read
   * @param emailId       Email ID Number of the email to mark as read, if there is no email in the incoming {@link Message}.
   */
  public void markAsRead(Message message, @Connection MailboxConnection connection,
                         @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder,
                         @Optional @Summary("Email ID of the email to mark as read") @DisplayName("Email ID") Integer emailId) {
    setFlagCommand.set(message, connection, mailboxFolder, emailId, SEEN);
  }

  /**
   * Marks an incoming email as DELETED, this way the marked email(s) are scheduled for deletion when the folder closes.
   * <p>
   * All DELETED marked emails are going to be eliminated from the mailbox when one of
   * {@link IMAPOperations#expungeFolder(MailboxConnection, String)} or
   * {@link CommonEmailOperations#delete(Message, MailboxConnection, String, Integer)} is executed.
   * <p>
   * This operation can target a single email, but also if the incoming {@link Message} is carrying a list of emails this
   * operation will mark all the emails that the {@link Message} is carrying.
   *
   * @param message       The incoming {@link Message}.
   * @param connection    The corresponding {@link MailboxConnection} instance.
   * @param mailboxFolder Mailbox folder where the emails are going to be marked as deleted
   * @param emailId       Email ID Number of the email to mark as deleted, if there is no email in the incoming {@link Message}.
   */
  public void markAsDeleted(Message message, @Connection MailboxConnection connection,
                            @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder,
                            @Optional @Summary("Email ID of the email to mark as deleted") @DisplayName("Email ID") Integer emailId) {
    setFlagCommand.set(message, connection, mailboxFolder, emailId, DELETED);
  }

  /**
   * Eliminates from the mailbox all the messages scheduled for deletion with the DELETED flag set.
   *
   * @param connection    The associated {@link MailboxConnection}.
   * @param mailboxFolder Mailbox folder where the emails with the 'DELETED' flag are going to be scheduled to be definitely
   *                      deleted
   */
  public void expungeFolder(@Connection MailboxConnection connection,
                            @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder) {
    expungeCommand.expunge(connection, mailboxFolder);
  }
}
