/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static java.lang.String.format;
import static java.nio.file.Paths.get;
import static javax.mail.Folder.READ_ONLY;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.closeFolder;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.getAttributesFromMessage;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.getOpenFolder;
import static org.mule.runtime.core.util.FileUtils.createFile;
import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.api.retriever.RetrieverConnection;
import org.mule.extension.email.internal.exception.EmailException;
import org.mule.extension.email.internal.exception.EmailRetrieverException;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.util.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Represents the store emails operation.
 *
 * @since 4.0
 */
public final class StoreCommand
{

    private static final String NO_ID_ERROR = "Expecting an explicit emailId value or email attributes in the incoming mule message in order to store an email.";

    //TODO: annotated the parameter directory with @Path when available

    /**
     * Stores the specified email of id {@code emailId} into the configured {@code directory}.
     * <p>
     * if no emailId is specified, the operation will try to find an email or {@link List} of emails
     * in the incoming {@link MuleMessage}.
     * <p>
     * If no email(s) are found in the {@link MuleMessage} and no {@code emailId} is specified.
     * the operation will fail.
     * <p>
     * The emails are stored as mime message in a ".txt" format.
     * <p>
     * The name of the email file is composed by the subject and
     * the received date of the email.
     *
     * @param connection  the associated {@link RetrieverConnection}.
     * @param muleMessage the incoming {@link MuleMessage}.
     * @param folderName  the name of the folder where the email(s) is going to be fetched.
     * @param directory   the directory where the emails are going to be stored.
     * @param emailId     the optional number of the email to be marked. for default the email is taken from the incoming {@link MuleMessage}.
     */
    public void store(RetrieverConnection connection, MuleMessage muleMessage, String folderName, String directory, Integer emailId)
    {
        try
        {
            Folder folder = getOpenFolder(folderName, READ_ONLY, connection.getStore());
            if (emailId == null)
            {
                Object payload = muleMessage.getPayload();
                if (payload instanceof List)
                {
                    for (Object o : (List) payload)
                    {
                        if (o instanceof MuleMessage)
                        {
                            emailId = getIdOrFail(((MuleMessage) o));
                            storeMessage(emailId, folder, directory);
                        }
                        else
                        {
                            throw new EmailException("Cannot perform operation for the incoming payload");
                        }
                    }
                    return;
                }
                emailId = getIdOrFail(muleMessage);
            }
            storeMessage(emailId, folder, directory);
            closeFolder(folder, false);
        }
        catch (MessagingException | IOException me)
        {
            throw new EmailRetrieverException(me);
        }
    }

    /**
     * Stores a single {@link Message} into the specified {@code directory}.
     *
     * @param emailId   the emailId corresponding to the message that is going to be stored
     * @param folder    the folder to look for the email
     * @param directory the directory where the message is going to be stored
     */
    private void storeMessage(int emailId, Folder folder, String directory) throws IOException, MessagingException
    {
        Message message = folder.getMessage(emailId);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.writeTo(outputStream);
        String fileName = format("%s-%s_%s.txt", message.getSubject(), message.getMessageNumber(), message.getReceivedDate());
        File emailFile = createFile(get(directory, fileName).toString());
        FileUtils.write(emailFile, outputStream.toString());
    }

    /**
     * Gets an emailId from a MuleMessage of fails if the MuleMessage does
     * not contains attributes of {@link EmailAttributes} type.
     */
    private int getIdOrFail(MuleMessage muleMessage)
    {
        return getAttributesFromMessage(muleMessage)
                .orElseThrow(() -> new EmailException(NO_ID_ERROR))
                .getId();
    }
}
