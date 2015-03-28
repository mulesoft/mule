/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.datetime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import junit.framework.Assert;

import org.junit.Test;

@SmallTest
public class DateTimeDateTestCase extends AbstractMuleTestCase
{

    private Calendar currentCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    private org.mule.api.el.datetime.Date now = new DateTime(currentCalendar).withTimeZone("UTC").getDate();

    @Test
    public void isBefore()
    {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        Assert.assertTrue(now.isBefore(new DateTime(cal).withTimeZone("UTC")));
    }

    @Test
    public void isAfter()
    {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        Assert.assertTrue(now.isAfter(new DateTime(cal).withTimeZone("UTC")));
    }

    @Test
    public void format()
    {
        SimpleDateFormat df = new SimpleDateFormat("EEE, MMM d, yyyy");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        Assert.assertEquals(df.format(now.toDate()), now.format("EEE, MMM d, yyyy"));
    }

    @Test
    public void timeZone()
    {
        Assert.assertEquals(TimeZone.getTimeZone("UTC").getDisplayName(), now.getTimeZone());
    }

    @Test
    public void plusDays()
    {
        Assert.assertEquals((currentCalendar.get(Calendar.DAY_OF_YEAR) % 365) + 1, now.plusDays(1).getDayOfYear());
    }

    @Test
    public void plusWeeks()
    {
        Assert.assertEquals((currentCalendar.get(Calendar.WEEK_OF_YEAR) % 52) + 1, now.plusWeeks(1).getWeekOfYear());
    }

    @Test
    public void plusMonths()
    {
        Assert.assertEquals(((currentCalendar.get(Calendar.MONTH) + 1) % 12) + 1, now.plusMonths(1).getMonth());
    }

    @Test
    public void plusYears()
    {
        Assert.assertEquals(currentCalendar.get(Calendar.YEAR) + 1, now.plusYears(1).getYear());
    }

    @Test
    public void withLocale()
    {
        SimpleDateFormat df = new SimpleDateFormat("E", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertEquals(df.format(new Date()), now.withLocale("en_US").format("E"));
    }

    @Test
    public void dayOfWeek()
    {
        assertEquals(currentCalendar.get(Calendar.DAY_OF_WEEK), now.getDayOfWeek());
    }

    @Test
    public void dayOfMonth()
    {
        assertEquals(currentCalendar.get(Calendar.DAY_OF_MONTH), now.getDayOfMonth());
    }

    @Test
    public void dayOfYear()
    {
        assertEquals(currentCalendar.get(Calendar.DAY_OF_YEAR), now.getDayOfYear());
    }

    @Test
    public void weekOfMonth()
    {
        assertEquals(currentCalendar.get(Calendar.WEEK_OF_MONTH), now.getWeekOfMonth());
    }

    @Test
    public void weekOfYear()
    {
        assertEquals(currentCalendar.get(Calendar.WEEK_OF_YEAR), now.getWeekOfYear());
    }

    @Test
    public void monthOfYear()
    {
        assertEquals(currentCalendar.get(Calendar.MONTH) + 1, now.getMonth());
    }

    @Test
    public void testToString()
    {
        assertEquals(DatatypeConverter.printDate(currentCalendar), now.toString());
    }

    @Test
    public void toDate()
    {
        assertEquals(Date.class, now.toDate().getClass());
    }

    @Test
    public void toCalendar()
    {
        assertEquals(GregorianCalendar.class, now.toCalendar().getClass());
    }

    @Test
    public void toXMLCalendar() throws DatatypeConfigurationException
    {
        assertTrue(now.toXMLCalendar() instanceof XMLGregorianCalendar);
    }

    @Test
    public void fromDate()
    {
        Date date = new Date();
        date.setYear(0);
        date.setMonth(0);
        date.setDate(1);
        assertEquals(1900, new DateTime(date).getYear());
        assertEquals(1, new DateTime(date).getMonth());
        assertEquals(1, new DateTime(date).getDayOfMonth());
    }

    @Test
    public void fromCalendar()
    {
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.YEAR, 1900);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        assertEquals(1900, new DateTime(cal).getYear());
        assertEquals(1, new DateTime(cal).getMonth());
        assertEquals(1, new DateTime(cal).getDayOfMonth());
    }

    @Test
    public void fromXMLCalendar() throws DatatypeConfigurationException
    {
        XMLGregorianCalendar xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(
            new GregorianCalendar());
        xmlCal.setYear(1900);
        xmlCal.setMonth(1);
        xmlCal.setDay(1);
        assertEquals(1900, new DateTime(xmlCal).getYear());
        assertEquals(1, new DateTime(xmlCal).getMonth());
        assertEquals(1, new DateTime(xmlCal).getDayOfMonth());
    }

}
