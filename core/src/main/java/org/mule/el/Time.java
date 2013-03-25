/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang.LocaleUtils;

public class Time extends AbstractInstant
{

    Time(AbstractInstant instant)
    {
        this.locale = instant.locale;
        this.calendar = instant.calendar;
    }

    public long getMilliSeconds()
    {
        return calendar.get(Calendar.MILLISECOND);
    }

    public boolean isBefore(Time date)
    {
        return calendar.before(date.calendar);
    }

    public boolean isAfter(Time date)
    {
        return calendar.after(date.calendar);
    }

    public String format()
    {
        return toString();
    }

    public String format(String pattern)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, locale);
        dateFormat.setTimeZone(calendar.getTimeZone());
        return dateFormat.format(calendar.getTime());
    }

    public String getZone()
    {
        return calendar.getTimeZone().getDisplayName(locale);
    }

    public Time plusMilliSeconds(int add)
    {
        calendar.add(Calendar.MILLISECOND, add);
        return this;
    }

    public Time plusSeconds(int add)
    {
        calendar.add(Calendar.SECOND, add);
        return this;
    }

    public Time plusMinutes(int add)
    {
        calendar.add(Calendar.MINUTE, add);
        return this;
    }

    public Time plusHours(int add)
    {
        calendar.add(Calendar.HOUR_OF_DAY, add);
        return this;
    }

    public Time withTimeZone(String newTimezone)
    {
        calendar.setTimeZone(TimeZone.getTimeZone(newTimezone));
        return this;
    }

    public Time withLocale(String locale)
    {
        this.locale = LocaleUtils.toLocale(locale);
        Calendar newCalendar = Calendar.getInstance(calendar.getTimeZone(), this.locale);
        newCalendar.setTime(calendar.getTime());
        this.calendar = newCalendar;
        return this;
    }

    public int getSeconds()
    {
        return calendar.get(Calendar.SECOND);
    }

    public int getMinutes()
    {
        return calendar.get(Calendar.MINUTE);
    }

    public int getHours()
    {
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    @Override
    public String toString()
    {
        return DatatypeConverter.printTime(calendar);
    }

}
