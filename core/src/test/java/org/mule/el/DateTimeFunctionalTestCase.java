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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.el.context.AbstractELTestCase;

import org.junit.Test;

public class DateTimeFunctionalTestCase extends AbstractELTestCase
{

    public DateTimeFunctionalTestCase(Variant variant)
    {
        super(variant);
    }

    @Test
    public void iso9601DateTimeRoundTrip()
    {
        assertTrue((Boolean) evaluate("time = server.dateTime; time == " + "dateTime(time.toString())"));
    }

    @Test
    public void iso9601DateRoundTrip()
    {
        assertTrue((Boolean) evaluate("time = server.dateTime.date; time == "
                                      + "dateTime(time.toString()).date"));
    }

    @Test
    public void iso9601TimeRoundTrip()
    {
        assertTrue((Boolean) evaluate("time = server.dateTime.time; time == "
                                      + "dateTime(time.toString()).time"));
    }

    @Test
    public void customFormatDateTimeRoundTrip()
    {
        assertTrue((Boolean) evaluate("time = server.dateTime.date; time == "
                                      + "dateTime(time.format('MM/dd/yy'),'MM/dd/yy')"));
    }

    @Test
    public void customFormatDateRoundTrip()
    {
        assertTrue((Boolean) evaluate("time = server.dateTime.date; time == "
                                      + "dateTime(time.format('MM/dd/yy'),'MM/dd/yy').date"));
    }

    @Test
    public void customFormatTimeRoundTrip()
    {
        assertTrue((Boolean) evaluate("time = server.dateTime.time; time == "
                                      + "dateTime(time.format('kkmmssSSS'),'kkmmssSSS').time"));
    }

    @Test
    public void dateRoundTrip()
    {
        assertTrue((Boolean) evaluate("time = server.dateTime; time == dateTime(time.toDate())"));
    }

    @Test
    public void calendarRoundTrip()
    {
        assertTrue((Boolean) evaluate("time = server.dateTime; time == dateTime(time.toCalendar())"));
    }

    @Test
    public void xmlCalendarRoundTrip()
    {
        assertTrue((Boolean) evaluate("time = server.dateTime; time == dateTime(time.toXMLCalendar())"));
    }

    @Test
    public void chainedManipulation()
    {
        assertTrue((Boolean) evaluate("time = server.dateTime; time == time.plusDays(365).plusYears(-1).plusHours(24)."
                                      + "plusDays(-2).plusMinutes(60).plusHours(-1)"));
    }

    @Test
    public void parsedTimeIsBeforeServerDateTime()
    {
        assertTrue((Boolean) evaluate("server.dateTime.isBefore(dateTime('2100-01-01'))"));
    }

    @Test
    public void parsedTimeIsAfterServerDateTime()
    {
        assertTrue((Boolean) evaluate("server.dateTime.isAfter(dateTime('1900-01-01'))"));
    }

    @Test
    public void addDaysAndCompareAfter()
    {
        assertTrue((Boolean) evaluate("server.dateTime.plusDays(1).isAfter(server.dateTime)"));
    }

    @Test
    public void substractDaysAndCompareBefore()
    {
        assertTrue((Boolean) evaluate("server.dateTime.plusDays(-1).isBefore(server.dateTime)"));
    }

    @Test
    public void iso8601ToSimpleDateFromat()
    {
        assertEquals("1/1/1900", evaluate("dateTime('1900-01-01').format('d/M/yyyy')"));
    }

    @Test
    public void simpleDateFromatToIso8601()
    {
        assertEquals("1900-01-01T00:00:00Z", evaluate("dateTime('1/1/1900','d/M/yyyy').withTimeZone('GMT').toString()"));
    }

    @Test
    public void changeIso8601TimeZone()
    {
        assertEquals("1900-01-01T00:00:00-08:00",
            evaluate("dateTime('1900-01-01').withTimeZone('PST').toString()"));
    }

    @Test
    public void addDays8601String()
    {
        assertEquals("1900-01-03T00:00:00Z", evaluate("dateTime('1900-01-01').withTimeZone('GMT').plusDays(2).toString()"));
    }

    @Test
    public void compareYearIsoAndSimpleDateFormatStrings()
    {
        assertTrue((Boolean) evaluate("dateTime('1900-01-01').year==dateTime('1/1/1900','d/M/yyyy').year"));
    }

    @Test
    public void isoStringEquals()
    {
        assertTrue((Boolean) evaluate("dateTime('1900-01-01') == dateTime('1900-01-01')"));
    }

    @Test
    public void isoAndSimpleDateFormatEquals()
    {
        assertTrue((Boolean) evaluate("dateTime('1900-01-01') == dateTime('1/1/1900','d/M/yyyy')"));
    }

    @Test
    public void equalsDate()
    {
        assertTrue((Boolean) evaluate("dateTime('1900-01-01') == dateTime('1900-01-01T23:11:34').date"));
    }

    @Test
    public void equalsDate2()
    {
        assertTrue((Boolean) evaluate("dateTime('1900-01-01T11:01:11').date == dateTime('1900-01-01T23:11:34').date"));
    }

    @Test
    public void equalsTime()
    {
        assertTrue((Boolean) evaluate("dateTime('23:11:34') == dateTime('1900-01-01T23:11:34').time"));
    }

    @Test
    public void equalsTime2()
    {
        assertTrue((Boolean) evaluate("dateTime('2100-12-12T23:11:34').time == dateTime('1900-01-01T23:11:34').time"));
    }

    @Test
    public void dateToString()
    {
        assertEquals("2100-12-12Z", evaluate("dateTime('2100-12-12T23:11:34').withTimeZone('GMT').date.format()"));
    }

    @Test
    public void timeToString()
    {
        assertEquals("23:11:34Z", evaluate("dateTime('2100-12-12T23:11:34').withTimeZone('GMT').time.format()"));
    }

}
