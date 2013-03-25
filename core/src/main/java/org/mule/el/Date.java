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

import java.util.Calendar;

import javax.xml.bind.DatatypeConverter;

public class Date extends AbstractInstant
{
    Date(DateTime dateTime)
    {
        this.calendar = dateTime.calendar;
        this.locale = dateTime.locale;
    }

    public int getDayOfWeek()
    {
        return calendar.get(Calendar.DAY_OF_WEEK);
    }

    public int getDayOfMonth()
    {
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public int getDayOfYear()
    {
        return calendar.get(Calendar.DAY_OF_YEAR);
    }

    public int getWeekOfMonth()
    {
        return calendar.get(Calendar.WEEK_OF_MONTH);
    }

    public int getWeekOfYear()
    {
        return calendar.get(Calendar.WEEK_OF_YEAR);
    }

    public int getMonth()
    {
        return calendar.get(Calendar.MONTH) + 1;
    }

    public int getYear()
    {
        return calendar.get(Calendar.YEAR);
    }

    public Date plusDays(int add)
    {
        calendar.add(Calendar.DAY_OF_YEAR, add);
        return this;
    }

    public Date plusWeeks(int add)
    {
        calendar.add(Calendar.WEEK_OF_YEAR, add);
        return this;
    }

    public Date plusMonths(int add)
    {
        calendar.add(Calendar.MONTH, add);
        return this;
    }

    public Date plusYears(int add)
    {
        calendar.add(Calendar.YEAR, add);
        return this;
    }

    @Override
    public String toString()
    {
        return DatatypeConverter.printDate(calendar);
    }

}
