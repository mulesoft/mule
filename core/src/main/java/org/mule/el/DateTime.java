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

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.el.context.ServerContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import javax.xml.bind.DatatypeConverter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Models a DateTime and simplifies the parsing/formatting and very basic manipulation of dates via Mule
 * expression language.
 */
public class DateTime extends AbstractInstant
{

    private Date date;
    private Time time;

    public DateTime(Calendar calendar, Locale locale)
    {
        this.locale = locale;
        this.calendar = calendar;
        this.date = new Date(this);
        this.time = new Time(this);
    }

    public DateTime()
    {
        this(Calendar.getInstance(ServerContext.getLocale()), (ServerContext.getLocale()));
    }

    public DateTime(Calendar calendar)
    {
        this(calendar, ServerContext.getLocale());
    }

    public DateTime(java.util.Date date)
    {
        this(Calendar.getInstance(ServerContext.getTimeZone(), ServerContext.getLocale()),
            ServerContext.getLocale());
        this.calendar.setTime(date);
    }

    public DateTime(XMLGregorianCalendar xmlCalendar)
    {
        this(xmlCalendar.toGregorianCalendar(), ServerContext.getLocale());
        if (xmlCalendar.getTimezone() == DatatypeConstants.FIELD_UNDEFINED)
        {
            calendar.setTimeZone(ServerContext.getTimeZone());
        }
    }

    public DateTime(String iso8601String)
    {
        this(datatypeFactory.newXMLGregorianCalendar(iso8601String).toGregorianCalendar(
            ServerContext.getTimeZone(), ServerContext.getLocale(), null), ServerContext.getLocale());
    }

    public DateTime(String dateString, String format) throws ParseException
    {
        this(Calendar.getInstance(ServerContext.getLocale()), ServerContext.getLocale());
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        this.calendar.setTime(dateFormat.parse(dateString));
        this.calendar.setTimeZone(ServerContext.getTimeZone());
    }

    @Override
    public String toString()
    {
        return DatatypeConverter.printDateTime(calendar);
    }

    public Calendar toCalendar()
    {
        return calendar;
    }

    public java.util.Date toDate()
    {
        return calendar.getTime();
    }

    public XMLGregorianCalendar toXMLCalendar() throws DatatypeConfigurationException
    {
        if (calendar instanceof GregorianCalendar)
        {
            return datatypeFactory.newXMLGregorianCalendar((GregorianCalendar) calendar);
        }
        else
        {
            throw new MuleRuntimeException(
                CoreMessages.createStaticMessage("org.mule.el.DateTime.toXMLCalendar() does not support non-gregorian calendars."));
        }
    }

    public int getDayOfWeek()
    {
        return date.getDayOfWeek();
    }

    public int getDayOfMonth()
    {
        return date.getDayOfMonth();
    }

    public int getDayOfYear()
    {
        return date.getDayOfYear();
    }

    public int getWeekOfMonth()
    {
        return date.getWeekOfMonth();
    }

    public int getWeekOfYear()
    {
        return date.getWeekOfYear();
    }

    public int getMonth()
    {
        return date.getMonth();
    }

    public int getYear()
    {
        return date.getYear();
    }

    public DateTime plusDays(int add)
    {
        date.plusDays(add);
        return this;
    }

    public DateTime plusWeeks(int add)
    {
        date.plusWeeks(add);
        return this;
    }

    public DateTime plusMonths(int add)
    {
        date.plusMonths(add);
        return this;
    }

    public DateTime plusYears(int add)
    {
        date.plusYears(add);
        return this;
    }

    public long getMilliSeconds()
    {
        return time.getMilliSeconds();
    }

    public DateTime plusMilliSeconds(int add)
    {
        time.plusMilliSeconds(add);
        return this;
    }

    public DateTime plusSeconds(int add)
    {
        time.plusSeconds(add);
        return this;
    }

    public DateTime plusMinutes(int add)
    {
        time.plusMinutes(add);
        return this;
    }

    public DateTime plusHours(int add)
    {
        time.plusHours(add);
        return this;
    }

    public int getSeconds()
    {
        return time.getSeconds();
    }

    public int getMinutes()
    {
        return time.getMinutes();
    }

    public int getHours()
    {
        return time.getHours();
    }

    public Date getDate()
    {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return date;
    }

    public Time getTime()
    {
        calendar.set(Calendar.YEAR, 1970);
        calendar.set(Calendar.DAY_OF_YEAR, 1);
        return time;
    }

}
