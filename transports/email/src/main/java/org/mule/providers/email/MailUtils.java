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

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang.StringUtils;

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
}
