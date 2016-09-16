/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.manager.pop3;

import static org.mule.extension.email.internal.util.EmailConnectorUtils.INBOX_FOLDER;
import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.extension.email.api.attributes.POP3EmailAttributes;
import org.mule.extension.email.api.predicate.POP3EmailPredicateBuilder;
import org.mule.extension.email.internal.commands.ListCommand;
import org.mule.extension.email.internal.manager.MailboxAccessConfiguration;
import org.mule.extension.email.internal.manager.MailboxConnection;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.OperationResult;

import java.util.List;

/**
 * Basic set of operations which perform on top the POP3 email protocol.
 *
 * @since 4.0
 */
public class POP3Operations {

  private final ListCommand listCommand = new ListCommand();

  /**
   * List all the emails in the configured pop3 mailBoxFolder that match with the specified {@code pop3Matcher} criteria.
   *
   * @param config        The {@link MailboxAccessConfiguration} associated to this operation.
   * @param connection    The corresponding {@link MailboxConnection} instance.
   * @param mailboxFolder Mailbox folder where the emails are going to be fetched
   * @param pop3Matcher   Email Matcher which gives the capability of filter the retrieved emails
   * @return an {@link OperationResult} {@link List} carrying all the emails content
   * and it's corresponding {@link BaseEmailAttributes}.
   */
  @Summary("List all the emails in the given POP3 Mailbox Folder")
  public List<OperationResult<Object, POP3EmailAttributes>> listPop3(@UseConfig POP3Configuration config,
                                                                     @Connection MailboxConnection connection,
                                                                     @Optional(defaultValue = INBOX_FOLDER) String mailboxFolder,
                                                                     @DisplayName("Matcher") @Optional POP3EmailPredicateBuilder pop3Matcher) {
    return listCommand.list(config, connection, mailboxFolder, pop3Matcher);
  }
}
