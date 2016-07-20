/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.retriever.imap;

import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Flags.Flag.SEEN;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.INBOX_FOLDER;
import org.mule.extension.email.internal.commands.ExpungeCommand;
import org.mule.extension.email.internal.commands.SetFlagCommand;
import org.mule.extension.email.internal.retriever.RetrieverConnection;
import org.mule.extension.email.internal.retriever.RetrieverOperations;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.List;

import javax.inject.Inject;

/**
 * Basic set of operations which perform on top the IMAP email protocol.
 *
 * @since 4.0
 */
public class IMAPOperations
{

    private final SetFlagCommand setFlagCommand = new SetFlagCommand();
    private final ExpungeCommand expungeCommand = new ExpungeCommand();

    @Inject
    private MuleContext context;

    /**
     * Marks the incoming emails as READ.
     * <p>
     * This operation targets all the emails specified by its id number,
     * marking all the emails that belong to the specified folder.
     *
     * @param emailIds      A list of the email numbers to look up in the folder.
     * @param connection    The corresponding {@link RetrieverConnection} instance.
     * @param mailboxFolder The folder where the emails are going to be fetched
     */
    public void markAsRead(@Optional(defaultValue = "#[payload]")
                           @Summary("Email ID Number of the emails to mark as read") List<Integer> emailIds,
                           @Connection RetrieverConnection connection,
                           @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder)
    {
        setFlagCommand.set(emailIds, connection, mailboxFolder, SEEN);
    }

    /**
     * Marks the incoming emails as DELETED, this way the marked email(s) are scheduled for deletion when the folder closes.
     * <p>
     * All DELETED marked emails are going to be eliminated from the mailbox when one of {@link IMAPOperations#expungeFolder}
     * or {@link RetrieverOperations#delete} is executed.
     * <p>
     * This operation targets all the emails specified by its id number.
     *
     * @param emailIds      a list of the email numbers to look up in the folder.
     * @param connection    The corresponding {@link RetrieverConnection} instance.
     * @param mailboxFolder The folder where the emails are going to be fetched
     */
    public void markAsDeleted(@Optional(defaultValue = "#[payload]")
                              @Summary("Email ID Number of the emails to mark as read") List<Integer> emailIds,
                              @Connection RetrieverConnection connection,
                              @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder)
    {
        setFlagCommand.set(emailIds, connection, mailboxFolder, DELETED);
    }

    /**
     * Eliminates from the mailbox all the messages scheduled for deletion with the DELETED flag set.
     *
     * @param connection    the associated {@link RetrieverConnection}.
     * @param mailboxFolder the folder where the emails are going to be fetched
     */
    public void expungeFolder(@Connection RetrieverConnection connection,
                              @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder)
    {
        expungeCommand.expunge(connection, mailboxFolder);
    }
}
