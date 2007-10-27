/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.properties;

import org.mule.MuleRuntimeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.UUID;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.Date;

/**  
 * This property extractor doesn't actually extract a property from the message, instead it allows for certain functions
 * to be called and returns a result. The functions it supports are -
 * <ul>
 * <li>now - returns an {@link java.sql.Timestamp} with the current time.</li>
 * <li>now - returns a {@link java.util.Date} with the current time.</li>
 * <li>uuid - returns a globally unique identifier</li>
 * <li>hostname - returns the hostname of the machine Mule is running on</li>
 * <li>ip - returns the ip address of the machine Mule is running on</li>
 * </ul>
 */
public class FunctionPropertyExtractor implements PropertyExtractor
{
    public static final String NAME = "function";

    public static final String NOW_FUNCTION = "now";
    public static final String DATE_FUNCTION = "date";
    public static final String UUID_FUNCTION = "uuid";
    public static final String HOSTNAME_FUNCTION = "hostname";
    public static final String IP_FUNCTION = "ip";

    public Object getProperty(String name, Object message)
    {
        if (name.equalsIgnoreCase(NOW_FUNCTION))
        {
            return new Timestamp(System.currentTimeMillis());
        }
        else if (name.equalsIgnoreCase(DATE_FUNCTION))
        {
            return new Date(System.currentTimeMillis());
        }
        else if (name.equalsIgnoreCase(UUID_FUNCTION))
        {
            return UUID.getUUID();
        }
        else if (name.equalsIgnoreCase(HOSTNAME_FUNCTION))
        {
            try
            {
                return InetAddress.getLocalHost().getHostName();
            }
            catch (UnknownHostException e)
            {
                throw new MuleRuntimeException(CoreMessages.failedToProcessExtractorFunction(name), e);
            }
        }
        else if (name.equalsIgnoreCase(IP_FUNCTION))
        {
            try
            {
                return InetAddress.getLocalHost().getHostAddress();
            }
            catch (UnknownHostException e)
            {
                throw new MuleRuntimeException(CoreMessages.failedToProcessExtractorFunction(name), e);
            }
        }
        else
        {
            throw new IllegalArgumentException(name);
        }
    }

    /**
     * Gts the name of the object
     *
     * @return the name of the object
     */
    public String getName()
    {
        return NAME;
    }

    /**
     * Sets the name of the object
     *
     * @param name the name of the object
     */
    public void setName(String name)
    {
        throw new UnsupportedOperationException("setName");
    }
}
