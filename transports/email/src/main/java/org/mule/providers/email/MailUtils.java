/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email;

import org.mule.config.i18n.CoreMessages;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

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

        StringBuffer buf = new StringBuffer(80);

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

        StringBuffer buf = new StringBuffer(80);

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

    public static void getAttachments(Multipart content, Map attachments) throws MessagingException, IOException
    {
        int x = 0;
        for(int i=0; i < content.getCount(); i++)
        {
            Part p = content.getBodyPart(i);
            if(p.getContentType().indexOf("multipart/mixed") > -1)
            {
                Multipart m = (Multipart)p.getContent();
                getAttachments(m, attachments);
            }
            else
            {
                String key;
                if(StringUtils.isNotEmpty(p.getDescription()))
                {
                    key = p.getDescription();
                }
                else if(StringUtils.isNotEmpty(p.getFileName()))
                {
                    key = p.getFileName();
                }
                else if(StringUtils.isNotEmpty(p.getDisposition()))
                {
                    key = p.getDisposition();
                }
                else
                {
                    key = String.valueOf(x++);
                }
                attachments.put(key, p);
            }
        }
    }
}
