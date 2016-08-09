/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static javax.mail.Folder.READ_WRITE;
import org.mule.extension.email.internal.retriever.RetrieverConnection;

/**
 * Represents the expungeFolder (eliminate completely) emails from folder operation.
 * <p>
 * Removes all the emails from a folder that contains the {@code DELETED} flag.
 *
 * @since 4.0
 */
public final class ExpungeCommand {

  /**
   * Removes from the mailbox all deleted messages if the flag is set true.
   *
   * @param connection the associated {@link RetrieverConnection}.
   * @param folderName the name of the folder that is going to erase the deleted emails.
   */
  public void expunge(RetrieverConnection connection, String folderName) {
    connection.getFolder(folderName, READ_WRITE);
    connection.closeFolder(true);
  }
}
