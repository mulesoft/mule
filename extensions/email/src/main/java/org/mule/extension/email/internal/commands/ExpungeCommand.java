/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static java.lang.String.format;
import static javax.mail.Folder.READ_WRITE;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.getOpenFolder;
import org.mule.extension.email.api.retriever.RetrieverConnection;
import org.mule.extension.email.internal.exception.EmailException;

import javax.mail.MessagingException;

/**
 * Represents the expunge (eliminate completely) emails from folder operation.
 *
 * @since 4.0
 */
public final class ExpungeCommand
{

    /**
     * Removes from the mailbox all deleted messages if the flag is set true.
     *
     * @param connection the associated {@link RetrieverConnection}.
     * @param folderName the name of the folder that is going to expunge the deleted emails.
     */
    public void expunge(RetrieverConnection connection, String folderName)
    {
        try
        {
            getOpenFolder(folderName, READ_WRITE, connection.getStore()).close(true);
        }
        catch (MessagingException e)
        {
            throw new EmailException(format("Error while expunging the emails from folder [%s]", folderName));
        }
    }
}
