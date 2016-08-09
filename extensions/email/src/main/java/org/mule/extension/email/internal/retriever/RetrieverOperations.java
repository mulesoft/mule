/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.retriever;

import static org.mule.extension.email.internal.util.EmailConnectorUtils.INBOX_FOLDER;

import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.api.EmailPredicateBuilder;
import org.mule.extension.email.internal.commands.DeleteCommand;
import org.mule.extension.email.internal.commands.ListCommand;
import org.mule.extension.email.internal.commands.StoreCommand;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.List;
import java.util.function.Predicate;

/**
 * A set of operations for all email configurations that aims to retrieve and manage emails in a folder.
 *
 * @since 4.0
 */
public class RetrieverOperations {

  private final ListCommand listCommand = new ListCommand();
  private final StoreCommand storeCommand = new StoreCommand();
  private final DeleteCommand deleteCommand = new DeleteCommand();


  /**
   * List all the emails in the configured mailBoxFolder that match with the specified {@code matchWith} criteria.
   *
   * @param config The {@link RetrieverConfiguration} associated to this operation.
   * @param connection The corresponding {@link RetrieverConnection} instance.
   * @param mailboxFolder Mailbox folder where the emails are going to be fetched
   * @param matcher Email Matcher which gives the capability of filter the retrieved emails
   * @return a {@link List} of {@link MuleMessage} carrying all the emails and it's corresponding attributes.
   */
  // TODO: ADD PAGINATION SUPPORT WHEN AVAILABLE
  @Summary("List all the emails in the given Mailbox Folder")
  public List<MuleMessage> list(@UseConfig RetrieverConfiguration config, @Connection RetrieverConnection connection,
                                @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder,
                                @Optional EmailPredicateBuilder matcher) {
    return listCommand.list(connection, mailboxFolder, config.isEagerlyFetchContent(), buildMatcher(matcher));
  }

  /**
   * Stores the specified email of id {@code emailId} into the configured {@code localDirectory}.
   * <p>
   * if no emailId is specified, the operation will try to find an email or {@link List} of emails in the incoming
   * {@link MuleMessage}.
   * <p>
   * If no email(s) are found in the {@link MuleMessage} and no {@code emailId} is specified. the operation will fail.
   * <p>
   * The emails are stored as mime message in a ".txt" format.
   * <p>
   * The name of the email file is composed by the subject and the received date of the email.
   *
   * @param connection The associated {@link RetrieverConnection}.
   * @param muleMessage The incoming {@link MuleMessage}.
   * @param mailboxFolder Name of the folder where the email(s) is going to be stored.
   * @param localDirectory Local directory where the emails are going to be stored.
   * @param fileName Name of the file that is going to be stored. The operation will append the email number and received date in
   *        the end.
   * @param emailId Email ID Number of the email to store. By default the email is taken from the incoming {@link MuleMessage}.
   * @param overwrite Whether to overwrite a file that already exist
   */
  // TODO: annotated the parameter localDirectory with @Path when available
  @Summary("Stores an specified email into a local directory")
  public void store(@Connection RetrieverConnection connection, MuleMessage muleMessage, String localDirectory,
                    @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder, @Optional String fileName,
                    @Optional @Summary("Email ID Number of the email to delete") @DisplayName("Email ID") Integer emailId,
                    @Optional(defaultValue = "false") @DisplayName("Should Overwrite") boolean overwrite) {
    storeCommand.store(connection, muleMessage, mailboxFolder, localDirectory, fileName, emailId, overwrite);
  }

  private Predicate<EmailAttributes> buildMatcher(EmailPredicateBuilder matcher) {
    return matcher != null ? matcher.build() : attributes -> true;
  }

  /**
   * Eliminates from the mailbox the email with id {@code emailId}, if no {@code emailId} is specified will look for incoming
   * emails in the {@link MuleMessage} it could be a single or multiple emails.
   * <p>
   * For IMAP mailboxes all the messages scheduled for deletion (marked as DELETED) will also be erased from the folder if the
   * operation succeed.
   * <p>
   * If no {@code emailId} is provided and no emails are found in the incoming {@link MuleMessage} this operation will fail and no
   * email is going to be erased from the folder, not even the ones marked as DELETED previously.
   *
   * @param message The incoming {@link MuleMessage}.
   * @param connection The corresponding {@link RetrieverConnection} instance.
   * @param mailboxFolder Mailbox folder where the emails are going to be deleted
   * @param emailId Email ID Number of the email to delete, if there is no email in the incoming {@link MuleMessage}.
   */
  @Summary("Deletes an email from the given Mailbox Folder")
  public void delete(MuleMessage message, @Connection RetrieverConnection connection,
                     @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder,
                     @Optional @Summary("Email ID Number of the email to delete") @DisplayName("Email ID") Integer emailId) {
    deleteCommand.delete(message, connection, mailboxFolder, emailId);
  }

}
