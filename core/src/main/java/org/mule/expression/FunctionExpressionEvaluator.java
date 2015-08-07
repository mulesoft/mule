/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.ClassUtils;
import org.mule.util.DateUtils;
import org.mule.util.NetworkUtils;
import org.mule.util.UUID;

import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This property extractor doesn't actually extract a property from the message, instead it allows for certain functions
 * to be called and returns a result. The functions it supports are -
 * <ul>
 * <li>now - returns an {@link java.sql.Timestamp} with the current time.</li>
 * <li>date - returns a {@link java.util.Date} with the current time.</li>
 * <li>dateStamp - returns a {@link java.lang.String} that contains the current date formatted according to {@link #DEFAULT_DATE_FORMAT}.</li>
 * <li>datestamp:dd-MM-yyyy - returns a {@link java.lang.String} that contains the current date formatted according to the format passed into the function.</li>
 * <li>uuid - returns a globally unique identifier</li>
 * <li>hostname - returns the hostname of the machine Mule is running on</li>
 * <li>ip - returns the ip address of the machine Mule is running on</li>
 * <li>count - returns a local count that will increment for each call.  If the server is re-started the counter will return to zero</li>
 * <li>payloadClass - Returns a fuly qualified class name of the payload as a string</li>
 * <li>shortPayloadClass - Returns just the class name of the payload as a string</li>
 * </ul>
 */
public class FunctionExpressionEvaluator extends AbstractExpressionEvaluator
{
    public static final String NAME = "function";

    public static final String DEFAULT_DATE_FORMAT = "dd-MM-yy_HH-mm-ss.SSS";

    /**
     * A local counter that will increment for each call.  If the server is re-started the
     * counter will return to zero
     */
    private final AtomicLong count = new AtomicLong(0);

    public static final String NOW_FUNCTION = "now";
    public static final String DATE_FUNCTION = "date";
    public static final String DATESTAMP_FUNCTION = "datestamp";
    public static final String SYSTIME_FUNCTION = "systime";
    public static final String UUID_FUNCTION = "uuid";
    public static final String HOSTNAME_FUNCTION = "hostname";
    public static final String IP_FUNCTION = "ip";
    public static final String COUNT_FUNCTION = "count";
    public static final String PAYLOAD_CLASS_FUNCTION = "payloadClass";
    public static final String SHORT_PAYLOAD_CLASS_FUNCTION = "shortPayloadClass";

    public Object evaluate(String name, MuleMessage message)
    {
        if (name.equalsIgnoreCase(NOW_FUNCTION))
        {
            return new Timestamp(System.currentTimeMillis());
        }
        else if (name.equalsIgnoreCase(DATE_FUNCTION))
        {
            return new Date(System.currentTimeMillis());
        }
        else if (name.toLowerCase().startsWith(DATESTAMP_FUNCTION))
        {
            String temp = name.substring(DATESTAMP_FUNCTION.length());
            if (temp.length() == 0)
            {
                return DateUtils.getTimeStamp(DEFAULT_DATE_FORMAT);
            }
            else
            {
                temp = temp.substring(1);
                return DateUtils.getTimeStamp(temp);
            }
        }
        else if (name.equalsIgnoreCase(UUID_FUNCTION))
        {
            return UUID.getUUID();
        }
        else if (name.equalsIgnoreCase(SYSTIME_FUNCTION))
        {
            return System.currentTimeMillis();
        }
        else if (name.equalsIgnoreCase(HOSTNAME_FUNCTION))
        {
            try
            {
                return NetworkUtils.getLocalHost().getHostName();
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
                return NetworkUtils.getLocalHost().getHostAddress();
            }
            catch (UnknownHostException e)
            {
                throw new MuleRuntimeException(CoreMessages.failedToProcessExtractorFunction(name), e);
            }
        }
        else if (name.equalsIgnoreCase(COUNT_FUNCTION))
        {
            return count.getAndIncrement();
        }
        else if (name.equalsIgnoreCase(PAYLOAD_CLASS_FUNCTION))
        {
            return message.getPayload().getClass().getName();
        }
        else if (name.equalsIgnoreCase(SHORT_PAYLOAD_CLASS_FUNCTION))
        {
            return ClassUtils.getClassName(message.getPayload().getClass());
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

}
