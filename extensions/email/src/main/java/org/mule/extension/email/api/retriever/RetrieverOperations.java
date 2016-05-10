/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.retriever;

import static org.mule.extension.email.internal.util.EmailConnectorUtils.INBOX_FOLDER;
import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.api.retriever.matcher.EmailPredicateBuilder;
import org.mule.extension.email.internal.commands.ListCommand;
import org.mule.extension.email.internal.commands.StoreCommand;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;

import java.util.List;
import java.util.function.Predicate;

import javax.inject.Inject;

/**
 * A set of operations for all email configurations that aims to list emails.
 *
 * @since 4.0
 */
public class RetrieverOperations
{

    //TODO: DELETE THIS WHEN MULE MESSAGE DOENST REQUIRE THE CONTEXT TO BE BUILT WITH ATTRIBUTES.
    @Inject
    private MuleContext context;

    private final ListCommand listCommand = new ListCommand();
    private final StoreCommand storeOperation = new StoreCommand();


    /**
     * List all the emails in the configured folder that match with the specified {@code matchWith} criteria.
     *
     * @param config     the {@link RetrieverConfiguration} associated to this operation.
     * @param connection the corresponding {@link RetrieverConnection} instance.
     * @param folder     the folder where the emails are going to be fetched
     * @param matchWith  a {@link EmailPredicateBuilder} used to filter the output emails.
     * @return a {@link List} of {@link MuleMessage} carrying all the emails and it's corresponding attributes.
     */
    //TODO: ADD PAGINATION SUPPORT WHEN AVAILABLE
    public List<MuleMessage<String, EmailAttributes>> list(@UseConfig RetrieverConfiguration config,
                                                           @Connection RetrieverConnection connection,
                                                           @Optional(defaultValue = INBOX_FOLDER) String folder,
                                                           @Optional EmailPredicateBuilder matchWith)
    {
        return listCommand.list(connection, context, folder, config.isEagerlyFetchContent(), buildMatcher(matchWith));
    }

    /**
     * Store the email from the configured mailbox that match with the specified {@code matchWith} criteria
     * in the specified {@code directory}.
     *
     * @param connection  the corresponding {@link RetrieverConnection} instance.
     * @param muleMessage the incoming {@link MuleMessage}.
     * @param folder      the folder where the emails are going to be fetched
     * @param directory   the directory where the email files are going to be stored
     * @param emailId     the optional number of the email to be marked. for default the email is taken from the incoming {@link MuleMessage}.
     */
    public void store(@Connection RetrieverConnection connection,
                      MuleMessage muleMessage,
                      String directory,
                      @Optional(defaultValue = INBOX_FOLDER) String folder,
                      @Optional Integer emailId)
    {
        storeOperation.store(connection, muleMessage, folder, directory, emailId);
    }

    private Predicate<EmailAttributes> buildMatcher(EmailPredicateBuilder matchWith)
    {
        return matchWith != null ? matchWith.build() : attributes -> true;
    }
}
