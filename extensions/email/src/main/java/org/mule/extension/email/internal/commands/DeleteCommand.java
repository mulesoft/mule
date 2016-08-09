/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static javax.mail.Flags.Flag.DELETED;
import org.mule.extension.email.internal.retriever.RetrieverConnection;
import org.mule.runtime.api.message.MuleMessage;

import java.util.List;

/**
 * Represents the delete command (erase completely) of a folder. This operation can erase a single or multiple specified emails
 * from a folder and will also erase all the emails that contains the {@code DELETED} flag.
 *
 * @since 4.0
 */
public final class DeleteCommand {

  private final SetFlagCommand setFlagCommand = new SetFlagCommand();

  /**
   * Eliminates from the mailbox the email with id {@code emailId} and all the emails scheduled for deletion (marked as DELETED
   * messages).
   * <p>
   * if no emailId is specified, the operation will try to find an email or {@link List} of emails in the incoming
   * {@link MuleMessage}.
   * <p>
   * If no {@code emailId} is provided and no emails are found in the incoming {@link MuleMessage} this operation will fail and no
   * email is going to be erased from the folder, not even the ones marked as DELETED previously.
   *
   * @param message the incoming {@link MuleMessage}.
   * @param connection the corresponding {@link RetrieverConnection} instance.
   * @param folderName the folder where the emails are going to be fetched
   * @param emailId an optional email number to look up in the folder.
   */
  public void delete(MuleMessage message, RetrieverConnection connection, String folderName, Integer emailId) {
    setFlagCommand.set(message, connection, folderName, emailId, DELETED);
    connection.closeFolder(true);
  }
}
