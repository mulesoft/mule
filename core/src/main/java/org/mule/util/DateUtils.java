/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import org.mule.config.i18n.CoreMessages;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <code>DateUtils</code> contains some useful methods dealing date/time
 * conversion, formatting etc.
 */
// @ThreadSafe
public class DateUtils extends org.apache.commons.lang.time.DateUtils
{

    public static String getTimeStamp(String format)
    {
        // Format the current time.
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        Date currentTime = new Date();
        return formatter.format(currentTime);
    }

    public static String formatTimeStamp(Date dateTime, String format)
    {
        // Format the current time.
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(dateTime);
    }

    public static String getStringFromDate(Date date, String format)
    {
        // converts from date to strin using the standard TIME_STAMP_FORMAT
        // pattern
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }

    public static Date getDateFromString(String date, String format)
    {
        // The date must always be in the format of TIME_STAMP_FORMAT
        // i.e. JAN 29 2001 22:50:40 GMT
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        ParsePosition pos = new ParsePosition(0);

        // Parse the string back into a Time Stamp.
        return formatter.parse(date, pos);
    }

    public static String getFormattedDuration(long mills)
    {
        long days = mills / 86400000;
        mills = mills - (days * 86400000);
        long hours = mills / 3600000;
        mills = mills - (hours * 3600000);
        long mins = mills / 60000;
        mills = mills - (mins * 60000);
        long secs = mills / 1000;
        mills = mills - (secs * 1000);

        StringBuffer bf = new StringBuffer(60);
        bf.append(days).append(" ").append(CoreMessages.days().getMessage()).append(", ");
        bf.append(hours).append(" ").append(CoreMessages.hours().getMessage()).append(", ");
        bf.append(mins).append(" ").append(CoreMessages.minutes().getMessage()).append(", ");
        bf.append(secs).append(".").append(mills).append(" ").append(CoreMessages.seconds().getMessage());
        return bf.toString();
    }

}
