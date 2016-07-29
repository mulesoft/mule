/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.retriever;

import static org.mule.extension.email.internal.util.EmailConnectorUtils.INBOX_FOLDER;
import org.mule.extension.email.api.Email;
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
 * A set of operations for all email configurations that aims to retrieve and manage
 * emails in a folder.
 *
 * @since 4.0
 */
public class RetrieverOperations
{

    private final ListCommand listCommand = new ListCommand();
    private final StoreCommand storeCommand = new StoreCommand();
    private final DeleteCommand deleteCommand = new DeleteCommand();


    /**
     * List all the emails in the configured mailBoxFolder that match with the specified {@code matchWith} criteria.
     *
     * @param config        The {@link RetrieverConfiguration} associated to this operation.
     * @param connection    The corresponding {@link RetrieverConnection} instance.
     * @param mailboxFolder Mailbox folder where the emails are going to be fetched
     * @param matcher       Email Matcher which gives the capability of filter the retrieved emails
     * @return a {@link List} of {@link MuleMessage} carrying all the emails and it's corresponding attributes.
     */
    //TODO: ADD PAGINATION SUPPORT WHEN AVAILABLE
    @Summary("List all the emails in the given Mailbox Folder")
    public List<Email> list(@UseConfig RetrieverConfiguration config,
                            @Connection RetrieverConnection connection,
                            @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder,
                            @Optional String contentEncoding,
                            @Optional EmailPredicateBuilder matcher)
    {
        return listCommand.list(connection, mailboxFolder, contentEncoding, config.isEagerlyFetchContent(), config.getDefaultCharset(), buildMatcher(matcher));
    }

    /**
     * Stores the specified emails into the configured {@code localDirectory}.
     * <p>
     * If no email(s) are found the operation will fail.
     * <p>
     * The emails are stored as mime message in a ".txt" format.
     * <p>
     * The name of the email file is composed by the subject and the received date of the email.
     *
     * @param connection     the associated {@link RetrieverConnection}.
     * @param emailIds       the id number of the emails to be marked.
     * @param mailboxFolder  the name of the folder where the email(s) is going to be fetched.
     * @param localDirectory the localDirectory where the emails are going to be stored.
     * @param fileName       the name of the file that is going to be stored. The operation will append the email number and received date in the end.
     * @param overwrite      if should overwrite a file that already exist or not.
     */
    // TODO: annotated the parameter localDirectory with @Path when available
    @Summary("Stores an specified email into a local directory")
    public void store(@Connection RetrieverConnection connection,
                      @DisplayName("Email IDs") @Optional(defaultValue = "#[payload]") List<Integer> emailIds,
                      String localDirectory,
                      @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder,
                      @Optional String fileName,
                      @Optional(defaultValue = "false") boolean overwrite)
    {
        storeCommand.store(connection, emailIds, mailboxFolder, localDirectory, fileName, overwrite);
    }

    private Predicate<EmailAttributes> buildMatcher(EmailPredicateBuilder matcher)
    {
        return matcher != null ? matcher.build() : attributes -> true;
    }

    /**
     * Eliminates from the mailbox the email with id {@code emailId}, if no {@code emailId} is
     * specified will look for incoming emails in the {@link MuleMessage} it could be a single or multiple emails.
     * <p>
     * For IMAP mailboxes all the messages scheduled for deletion (marked as DELETED) will also be erased from the folder
     * if the operation succeed.
     * <p>
     * If no {@code emailIds} are provided this operation will fail and no email is going to be erased from the folder,
     * not even the ones marked as DELETED previously.
     *
     * @param emailIds      the id number of the emails to look up in the folder for deletion.
     * @param connection    the corresponding {@link RetrieverConnection} instance.
     * @param mailboxFolder the folder where the emails are going to be fetched
     */
    @Summary("Deletes an email from the given Mailbox Folder")
    public void delete(@DisplayName("Email IDs") @Optional(defaultValue = "#[payload]") List<Integer> emailIds,
                       @Connection RetrieverConnection connection,
                       @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder)
    {
        deleteCommand.delete(emailIds, connection, mailboxFolder);
    }

}
