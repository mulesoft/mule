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
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.lang.LocaleUtils;

public abstract class AbstractInstant
{

    protected Locale locale;
    protected Calendar calendar;

    protected static final DatatypeFactory datatypeFactory;

    static
    {
        try
        {
            datatypeFactory = DatatypeFactory.newInstance();
        }
        catch (DatatypeConfigurationException e)
        {
            throw new Error(e);
        }
    }

    public boolean isBefore(AbstractInstant date)
    {
        return calendar.before(date.calendar);
    }

    public boolean isAfter(AbstractInstant date)
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

    public AbstractInstant withTimeZone(String newTimezone)
    {
        calendar.setTimeZone(TimeZone.getTimeZone(newTimezone));
        return this;
    }

    public AbstractInstant withLocale(String locale)
    {
        this.locale = LocaleUtils.toLocale(locale);
        Calendar newCalendar = Calendar.getInstance(calendar.getTimeZone(), this.locale);
        newCalendar.setTime(calendar.getTime());
        this.calendar = newCalendar;
        return this;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof AbstractInstant))
        {
            return false;
        }
        else
        {
            AbstractInstant other = (AbstractInstant) obj;
            return calendar.getTimeInMillis() == other.calendar.getTimeInMillis()
                   && calendar.getFirstDayOfWeek() == other.calendar.getFirstDayOfWeek()
                   && calendar.getFirstDayOfWeek() == other.calendar.getFirstDayOfWeek()
                   && calendar.getTimeZone().getOffset(calendar.getTimeInMillis()) == other.calendar.getTimeZone()
                       .getOffset(other.calendar.getTimeInMillis());
        }
    }
}
