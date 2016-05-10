/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static javax.mail.Part.ATTACHMENT;
import static org.mule.runtime.core.util.IOUtils.toByteArray;
import org.mule.extension.email.api.EmailAttachment;
import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.internal.exception.EmailException;
import org.mule.runtime.api.message.MuleMessage;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Store;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * this class contains common methods for email handling.
 *
 * @since 4.0
 */
public final class EmailConnectorUtils
{
    public static final String ATTRIBUTES_NOT_FOUND_MASK = "No email attributes were found in the incoming message. %s";
    public static final String INBOX_FOLDER = "INBOX";

    /**
     * Hide constructor
     */
    private EmailConnectorUtils()
    {
    }

    /**
     * Converts a simple {@link String} representing an address into an {@link InternetAddress} instance
     *
     * @param address the string to be converted.
     * @return a new {@link InternetAddress} instance.
     */
    public static Address toAddress(String address)
    {
        try
        {
            return new InternetAddress(address);
        }
        catch (AddressException e)
        {
            throw new EmailException(format("Error while creating %s InternetAddress", address));
        }
    }

    /**
     * Converts a {@link List} of {@link String}s representing email addresses into an {@link InternetAddress} array.
     *
     * @param addresses the list to be converted.
     * @return a new {@link Address}[] instance.
     */
    public static Address[] toAddressArray(List<String> addresses)
    {
        return addresses.stream().map(EmailConnectorUtils::toAddress).toArray(Address[]::new);
    }

    /**
     * Extracts the incoming {@link MuleMessage} attributes of {@link EmailAttributes} type.
     *
     * @param muleMessage      the incoming {@link MuleMessage}.
     * @return an {@link Optional} value with the {@link EmailAttributes}.
     */
    public static Optional<EmailAttributes> getAttributesFromMessage(MuleMessage muleMessage)
    {
        if (muleMessage.getAttributes() instanceof EmailAttributes)
        {
            return Optional.ofNullable((EmailAttributes) muleMessage.getAttributes());
        }
        return Optional.empty();
    }

    /**
     * Transforms a {@link Map} of attachments into a {@link List} of {@link EmailAttachment}s.
     *
     * @param attachments a {@link Map} of {@link String} {@link DataHandler} that carries the attachments.
     * @return a {@link List} of {@link EmailAttachment}.
     */
    public static List<EmailAttachment> mapToEmailAttributes(Map<String, DataHandler> attachments)
    {
        return attachments.entrySet().stream()
                .map(e -> new EmailAttachment(e.getKey(), e.getValue(), e.getValue().getContentType()))
                .collect(toList());
    }

    /**
     * Extracts the text body of an email part.
     *
     * @param part the part to getPropertiesInstance the body from.
     * @return the body of the part.
     */
    public static String getTextBody(Part part)
    {
        try
        {
            Object partContent = part.getContent();
            if (isMultipart(partContent))
            {
                Multipart mp = (Multipart) partContent;
                for (int i = 0; i < mp.getCount(); i++)
                {
                    BodyPart bodyPart = mp.getBodyPart(i);
                    Object bodyPartContent = bodyPart.getContent();
                    if (isText(bodyPartContent) && !isAttachment(bodyPart))
                    {
                        return bodyPartContent.toString();
                    }
                }
            }

            if (isText(partContent))
            {
                return partContent.toString();
            }

            return "";
        }
        catch (Exception e)
        {
            throw new EmailException(e.getMessage(), e);
        }
    }

    /**
     * Extracts the attachments of an email part.
     *
     * @param part the part to getPropertiesInstance the attachments from.
     * @return a {@link Map} with the email attachments.
     */
    public static Map<String, DataHandler> getAttachments(Part part)
    {
        Map<String, DataHandler> attachments = new HashMap<>();
        try
        {
            Object content = part.getContent();
            if (isMultipart(content))
            {
                Multipart mp = (Multipart) content;
                for (int i = 0; i < mp.getCount(); i++)
                {
                    attachments.putAll(getAttachments(mp.getBodyPart(i)));
                }
            }

            String filename = part.getFileName();
            if (isAttachment(part) && filename != null)
            {
                DataHandler dataHandler = part.getDataHandler();
                DataHandler attachmentDatahandler = new DataHandler(new ByteArrayInputStream(toByteArray(part.getInputStream())), dataHandler.getContentType());
                attachments.put(filename, attachmentDatahandler);
            }
        }
        catch (Exception e)
        {
            throw new EmailException(e.getMessage(), e);
        }

        return attachments;
    }

    /**
     * Opens and return the email {@link Folder} of name {@code folderName}.
     * The folder can contain Messages, other Folders or both.
     *
     * @param folderName the name of the folder to be opened.
     * @param mode       open the folder READ_ONLY or READ_WRITE
     * @return the opened {@link Folder}
     */
    public static Folder getOpenFolder(String folderName, int mode, Store store)
    {
        try
        {
            Folder folder = store.getFolder(folderName);
            if (!folder.isOpen())
            {
                folder.open(mode);
            }
            return folder;
        }
        catch (MessagingException e)
        {
            throw new EmailException(format("Error while opening folder %s", folderName), e);
        }
    }

    /**
     * Closes the specified {@code folder}
     *
     * @param folder the folder to be closed.
     */
    public static void closeFolder(Folder folder, boolean expunge)
    {
        try
        {
            folder.close(expunge);
        }
        catch (MessagingException e)
        {
            throw new EmailException(format("Error while closing folder %s", folder.getName()), e);
        }
    }

    /**
     * Evaluates whether a {@link Part} is dispositioned as an attachment or not.
     *
     * @param part the part to be validated.
     * @return true is the part is dispositioned as an attachment, false otherwise
     * @throws MessagingException
     */
    private static boolean isAttachment(Part part) throws MessagingException
    {
        return part.getDisposition() != null && part.getDisposition().equals(ATTACHMENT);
    }

    /**
     * Evaluates whether a content is multipart or not.
     *
     * @param content the content to be evaluated.
     * @return true if is multipart, false otherwise
     */
    private static boolean isMultipart(Object content)
    {
        return content instanceof Multipart;
    }

    /**
     * Evaluates whether a content is text or not.
     *
     * @param content the content to be evaluated.
     * @return true if is text, false otherwise
     */
    private static boolean isText(Object content)
    {
        return content instanceof String;
    }
}
