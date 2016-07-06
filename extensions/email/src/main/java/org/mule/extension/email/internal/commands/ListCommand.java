/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static javax.mail.Folder.READ_ONLY;
import static org.mule.extension.email.internal.builder.EmailAttributesBuilder.fromMessage;

import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.internal.EmailContentProcessor;
import org.mule.extension.email.internal.exception.EmailRetrieverException;
import org.mule.extension.email.internal.retriever.RetrieverConnection;
import org.mule.runtime.api.message.MuleMessage;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Represents the list emails operation.
 *
 * @since 4.0
 */
public final class ListCommand
{

    /**
     * Retrieves all the emails in the specified {@code folderName}.
     * <p>
     * A new {@link MuleMessage} is created for each fetched email from the folder, where
     * the payload is the text body of the email and the other metadata is carried by
     * an {@link EmailAttributes} instance.
     * <p>
     * For folder implementations (like IMAP) that support fetching without reading the content, if the content
     * should NOT be read ({@code readContent} = false) the SEEN flag is not going to be set.
     *
     * @param connection  the associated {@link RetrieverConnection}.
     * @param folderName  the name of the folder where the emails are stored.
     * @param readContent if should read the email content or not.
     * @param matcher     a {@link Predicate} of {@link EmailAttributes} used to filter the output list  @return a {@link List} of {@link MuleMessage} carrying all the emails and it's corresponding attributes.
     */
    public List<MuleMessage> list(RetrieverConnection connection,
                                  String folderName,
                                  boolean readContent,
                                  Predicate<EmailAttributes> matcher)
    {
        try
        {
            Folder folder = connection.getFolder(folderName, READ_ONLY);
            List<MuleMessage> list = new LinkedList<>();
            for (Message m : folder.getMessages())
            {
                String body = "";
                EmailAttributes attributes = fromMessage(m, false);
                if (matcher.test(attributes))
                {
                    if (readContent)
                    {
                        body = EmailContentProcessor.process(m).getBody();
                        attributes = fromMessage(m);
                    }
                    list.add(MuleMessage.builder().payload(body).attributes(attributes).build());
                }
            }
            return list;
        }
        catch (MessagingException me)
        {
            throw new EmailRetrieverException(me);
        }
    }
}
