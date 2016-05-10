/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static java.lang.String.format;
import static javax.mail.Folder.READ_WRITE;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.closeFolder;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.getAttributesFromMessage;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.getOpenFolder;
import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.api.retriever.RetrieverConnection;
import org.mule.extension.email.internal.exception.EmailException;
import org.mule.runtime.api.message.MuleMessage;

import java.util.List;

import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Represents the set flag operation.
 *
 * @since 4.0
 */
public class SetFlagCommand
{

    private static final String NO_ID_ERROR_MASK = "Expecting an explicit emailId value or email attributes " +
                                                   "in the incoming mule message in order to set the [%s] flag.";

    private static final String SET_FLAG_ERROR_MESSAGE_MASK = "Error while fetching email id [%s] in order to set the flag [%s]";

    /**
     * Sets the specified {@code flag} into the email of number {@code emailId}
     * <p>
     * if no emailId is specified, the operation will try to find an email or {@link List} of emails
     * in the incoming {@link MuleMessage}.
     * <p>
     * If no email(s) are found in the {@link MuleMessage} and no {@code emailId} is specified.
     * the operation will fail.
     *
     * @param muleMessage the incoming {@link MuleMessage}.
     * @param connection  the associated {@link RetrieverConnection}.
     * @param folderName  the name of the folder where the email(s) is going to be fetched.
     * @param emailId     the optional number of the email to be marked. for default the email is taken from the incoming {@link MuleMessage}.
     * @param flag        the flag to be setted.
     */
    public void set(MuleMessage muleMessage, RetrieverConnection connection,  String folderName, Integer emailId, Flag flag)
    {
        Folder folder = getOpenFolder(folderName, READ_WRITE, connection.getStore());
        Object payload = muleMessage.getPayload();

        if (emailId == null)
        {
            if (payload instanceof List)
            {
                for (Object o : (List) payload)
                {
                    if (o instanceof MuleMessage)
                    {
                        emailId = getIdOrFail((MuleMessage) o, flag);
                        setFlag(folder, emailId, flag);
                    }
                    else
                    {
                        //TODO Better Message
                        throw new IllegalArgumentException("Cannot perform operation for the incoming payload");
                    }
                }
                return;
            }
            emailId = getIdOrFail(muleMessage, flag);
        }
        setFlag(folder, emailId, flag);
        closeFolder(folder, false);
    }

    /**
     * Gets an emailId from a MuleMessage of fails if the MuleMessage does
     * not contains attributes of {@link EmailAttributes} type.
     */
    private int getIdOrFail(MuleMessage muleMessage, Flag flag)
    {
        return getAttributesFromMessage(muleMessage)
                .orElseThrow(() -> new IllegalArgumentException(format(NO_ID_ERROR_MASK, flag.toString())))
                .getId();
    }

    /**
     * Sets the specified {@code flag} into an email with the specified {@code emailNumber}.
     *
     * @param folder      the configured folder.
     * @param emailId     the optional number of the email to be marked. for default the email is taken from the incoming {@link MuleMessage}.
     * @param flag        the flag to be setted.
     */
    private void setFlag(Folder folder, Integer emailId, Flag flag)
    {
        try
        {
            Message message = folder.getMessage(emailId);
            message.setFlag(flag, true);
        }
        catch (MessagingException e)
        {
            throw new EmailException(format(SET_FLAG_ERROR_MESSAGE_MASK, emailId, flag.toString()), e);
        }
    }
}
