/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.mailbox.imap;

import static java.lang.String.format;
import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Flags.Flag.SEEN;
import static javax.mail.Folder.READ_WRITE;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.DEFAULT_PAGE_SIZE;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.INBOX_FOLDER;
import static org.mule.extension.email.internal.util.EmailConnectorConstants.PAGE_SIZE_ERROR_MESSAGE;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import org.mule.extension.email.api.EmailMetadataKey;
import org.mule.extension.email.api.EmailMetadataResolver;
import org.mule.extension.email.api.attributes.IMAPEmailAttributes;
import org.mule.extension.email.api.exception.EmailAccessingFolderErrorTypeProvider;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.api.exception.EmailMarkingErrorTypeProvider;
import org.mule.extension.email.api.predicate.IMAPEmailPredicateBuilder;
import org.mule.extension.email.internal.commands.ExpungeCommand;
import org.mule.extension.email.internal.commands.PagingProviderEmailDelegate;
import org.mule.extension.email.internal.commands.SetFlagCommand;
import org.mule.extension.email.internal.commands.StoreCommand;
import org.mule.extension.email.internal.mailbox.MailboxAccessConfiguration;
import org.mule.extension.email.internal.mailbox.MailboxConnection;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;

import java.util.List;

import javax.mail.MessagingException;

/**
 * Basic set of operations which perform on top the IMAP email protocol.
 *
 * @since 4.0
 */
public class IMAPOperations {

  private final ExpungeCommand expungeCommand = new ExpungeCommand();
  private final StoreCommand storeCommand = new StoreCommand();
  private final SetFlagCommand setFlagCommand = new SetFlagCommand();

  /**
   * List all the emails (with pagination) in the configured imap mailBoxFolder that match with the specified {@code imapMatcher}
   * criteria.
   *
   * @param config The {@link MailboxAccessConfiguration} associated to this operation.
   * @param connection The corresponding {@link MailboxConnection} instance.
   * @param mailboxFolder Mailbox folder where the emails are going to be fetched
   * @param imapMatcher Email Matcher which gives the capability of filter the retrieved emails
   * @param deleteAfterRetrieve Specifies if the returned emails must be deleted after being retrieved or not.
   * @return an {@link PagingProvider} composed with an {@link Result} with a {@link List} carrying all the emails content and
   *         it's corresponding {@link IMAPEmailAttributes}.
   */
  @Summary("List all the emails in the given POP3 Mailbox Folder")
  @OutputResolver(output = EmailMetadataResolver.class)
  public PagingProvider<MailboxConnection, Result<Object, IMAPEmailAttributes>> listImap(@UseConfig IMAPConfiguration config,
                                                                                         @Connection MailboxConnection connection,
                                                                                         @Optional(
                                                                                             defaultValue = INBOX_FOLDER) String mailboxFolder,
                                                                                         @DisplayName("Match with") @Optional IMAPEmailPredicateBuilder imapMatcher,
                                                                                         @Optional(
                                                                                             defaultValue = "false") boolean deleteAfterRetrieve,
                                                                                         @MetadataKeyId @Optional(
                                                                                             defaultValue = "ANY") @Placement(
                                                                                                 tab = ADVANCED_TAB) EmailMetadataKey outputType,
                                                                                         @Optional(
                                                                                             defaultValue = DEFAULT_PAGE_SIZE) int pageSize) {
    checkArgument(pageSize > 0, format(PAGE_SIZE_ERROR_MESSAGE, pageSize));
    return new PagingProviderEmailDelegate<>(config, mailboxFolder, imapMatcher, pageSize, deleteAfterRetrieve,
                                             attributes -> setFlagCommand.setByUID(connection, mailboxFolder, DELETED,
                                                                                   attributes.getId()));
  }

  /**
   * Marks a single email as READ changing it's state in the specified mailbox folder.
   * <p>
   * This operation can targets a single email.
   *
   * @param connection The corresponding {@link MailboxConnection} instance.
   * @param mailboxFolder Folder where the emails are going to be marked as read
   * @param emailId Email ID Number of the email to mark as read.
   */
  @Throws(EmailMarkingErrorTypeProvider.class)
  public void markAsRead(@Connection MailboxConnection connection,
                         @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder,
                         @Summary("Email ID Number of the email to mark as read") @DisplayName("Email ID") long emailId) {
    setFlagCommand.setByUID(connection, mailboxFolder, SEEN, emailId);
  }

  /**
   * Marks an incoming email as DELETED, this way the marked email(s) are scheduled for deletion when the folder closes, this
   * means that the email is not physically eliminated from the mailbox folder, but it's state changes.
   * <p>
   * All DELETED marked emails are going to be eliminated from the mailbox when one of
   * {@link IMAPOperations#expungeFolder(MailboxConnection, String)} or
   * {@link IMAPOperations#delete(MailboxConnection, String, long)} is executed.
   * <p>
   * This operation targets a single email.
   *
   * @param connection The corresponding {@link MailboxConnection} instance.
   * @param mailboxFolder Mailbox folder where the emails are going to be marked as deleted
   * @param emailId Email ID Number of the email to mark as deleted.
   */
  @Throws(EmailMarkingErrorTypeProvider.class)
  public void markAsDeleted(@Connection MailboxConnection connection,
                            @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder,
                            @Summary("Email ID Number of the email to mark as deleted") @DisplayName("Email ID") long emailId) {
    setFlagCommand.setByUID(connection, mailboxFolder, DELETED, emailId);
  }

  /**
   * Eliminates from the mailbox all the messages scheduled for deletion with the DELETED flag set.
   *
   * @param connection The associated {@link MailboxConnection}.
   * @param mailboxFolder Mailbox folder where the emails with the 'DELETED' flag are going to be scheduled to be definitely
   *        deleted
   */

  @Throws(EmailAccessingFolderErrorTypeProvider.class)
  public void expungeFolder(@Connection MailboxConnection connection,
                            @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder) {
    expungeCommand.expunge(connection, mailboxFolder);
  }

  /**
   * Stores the specified email of id {@code emailId} into the configured {@code localDirectory}.
   * <p>
   * The emails are stored as mime message in a ".txt" format.
   * <p>
   * The name of the email file is composed by the subject and the received date of the email.
   *
   * @param connection The associated {@link MailboxConnection}.
   * @param mailboxFolder Name of the folder where the email(s) is going to be stored.
   * @param localDirectory Local directory where the emails are going to be stored.
   * @param fileName Name of the file that is going to be stored. The operation will append the email number and received date in
   *        the end.
   * @param emailId Email ID Number of the email to store.
   * @param overwrite Whether to overwrite a file that already exist
   */
  // TODO: annotated the parameter localDirectory with @Path when available
  @Summary("Stores an specified email into a local directory")
  public void store(@Connection MailboxConnection connection, String localDirectory,
                    @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder, @Optional String fileName,
                    @Summary("Email ID Number of the email to delete") @DisplayName("Email ID") long emailId,
                    @Optional(defaultValue = "false") @DisplayName("Should Overwrite") boolean overwrite) {
    storeCommand.store(connection, mailboxFolder, localDirectory, fileName, emailId, overwrite);
  }

  /**
   * Eliminates from the mailbox the email with id {@code emailId}.
   * <p>
   * For IMAP mailboxes all the messages scheduled for deletion (marked as DELETED) will also be erased from the folder.
   *
   * @param connection The corresponding {@link MailboxConnection} instance.
   * @param mailboxFolder Mailbox folder where the emails are going to be deleted
   * @param emailId Email ID Number of the email to delete.
   */
  @Summary("Deletes an email from the given Mailbox Folder")
  @Throws(EmailMarkingErrorTypeProvider.class)
  public void delete(@Connection MailboxConnection connection,
                     @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder,
                     @Summary("Email ID Number of the email to delete") @DisplayName("Email ID") long emailId) {
    markAsDeleted(connection, mailboxFolder, emailId);
    try {
      connection.getFolder(mailboxFolder, READ_WRITE).close(true);
    } catch (MessagingException e) {
      throw new EmailException(format("Error while eliminating email uid:[%s] from the [%s] folder", emailId, mailboxFolder), e);
    }
  }


}
