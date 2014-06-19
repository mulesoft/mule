/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email;

import org.mule.config.i18n.CoreMessages;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;

/**
 * Contains javax.mail helpers.
 */
public class MailUtils
{
    public static String internetAddressesToString(InternetAddress[] addresses)
    {
        if (addresses == null || addresses.length == 0)
        {
            return StringUtils.EMPTY;
        }

        StringBuilder buf = new StringBuilder(80);

        for (int i = 0; i < addresses.length; i++)
        {
            InternetAddress address = addresses[i];
            buf.append(address.getAddress());
            // all except the last one
            if (i < addresses.length - 1)
            {
                buf.append(", ");
            }
        }

        return buf.toString();
    }

    public static String internetAddressesToString(InternetAddress address)
    {
        return internetAddressesToString(new InternetAddress[]{address});
    }

    public static String mailAddressesToString(Address[] addresses)
    {
        if (addresses == null || addresses.length == 0)
        {
            return StringUtils.EMPTY;
        }

        StringBuilder buf = new StringBuilder(80);

        for (int i = 0; i < addresses.length; i++)
        {
            Address address = addresses[i];
            buf.append(address.toString());
            // all except the last one
            if (i < addresses.length - 1)
            {
                buf.append(", ");
            }
        }

        return buf.toString();
    }

    public static String mailAddressesToString(Address address)
    {
        return mailAddressesToString(new Address[]{address});
    }

    public static InternetAddress[] stringToInternetAddresses(String address) throws AddressException
    {
        if (StringUtils.isNotBlank(address))
        {
            return InternetAddress.parse(address, false);
        }
        else
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("Email address").toString());
        }
    }

    public static void getAttachments(Multipart content, Map<String, Part> attachments) throws MessagingException, IOException
    {
        int x = 0;
        for(int i = 0; i < content.getCount(); i++)
        {
            Part part = content.getBodyPart(i);
            if (part.getContentType().indexOf("multipart/mixed") > -1)
            {
                Multipart m = (Multipart) part.getContent();
                getAttachments(m, attachments);
            }
            else
            {
                String key;
                if (StringUtils.isNotEmpty(part.getDescription()))
                {
                    key = part.getDescription();
                }
                else if (StringUtils.isNotEmpty(part.getFileName()))
                {
                    try
                    {
                        key = MimeUtility.decodeText(part.getFileName());
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        key = part.getFileName();
                    }
                }
                else if (StringUtils.isNotEmpty(part.getDisposition()))
                {
                    key = part.getDisposition();
                }
                else
                {
                    key = String.valueOf(x++);
                }

                key = getAttachmentName(key, attachments);

                attachments.put(key, part);
            }
        }
    }


    /**
     * Check whether an attachment with the same name already exists,
     * using a counter for override protection.
     *
     * @param key A attachment name
     * @param attachments Map with attachments
     * @return attachment name
     */
    public static String getAttachmentName(String key, Map<String, Part> attachments)
    {
        if (attachments.containsKey(key))
        {
            int x = 0;
            while (attachments.containsKey(x + "_" + key))
            {
                x++;
            }
            return x + "_" + key;

        } else
        {
            return key;
        }
    }

    /**
     * Check whether a property name has the format associated with a list
     * of header values
     *
     * @param name A property name
     * @return true if the name is associated with a list of header values
     *         (more exactly, if it starts with HEADER_LIST_PREFIX, which gives an
     *         invalid header name according to RFC822).
     */
    public static boolean isListHeader(String name)
    {
        return null != name && name.startsWith(MailMuleMessageFactory.HEADER_LIST_PREFIX);
    }

    /**
     * Convert a header name to the property name associated with a list of
     * header values (ie prepend the prefix)
     *
     * @param header A header name
     * @return The associated list property name (ie with HEADER_LIST_PREFIX prepended)
     */
    public static String toListHeader(String header)
    {
        if (isListHeader(header))
        {
            return header;
        }
        else
        {
            return MailMuleMessageFactory.HEADER_LIST_PREFIX + header;
        }
    }
}
