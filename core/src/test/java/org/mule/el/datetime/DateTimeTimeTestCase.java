/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.datetime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.api.el.datetime.Time;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

@SmallTest
public class DateTimeTimeTestCase extends AbstractMuleTestCase
{

    protected Time now = new DateTime().withTimeZone("UTC").getTime();

    @Test
    public void milliSeconds()
    {
        Assert.assertTrue(now.getMilliSeconds() < 1000);
    }

    @Test
    public void isBefore()
    {System.out.println(now.getHours());
        if (now.getHours() == 23)
        {
            Assert.assertTrue((Boolean) now.isAfter(new DateTime().changeTimeZone("UTC")
                .plusHours(1)
                .getTime()));
        }
        else
        {
            Assert.assertTrue((Boolean) now.isBefore(new DateTime().changeTimeZone("UTC")
                .plusHours(1)
                .getTime()));
        }
    }

    @Test
    public void isAfter()
    {
        if (now.getHours() == 0)
        {
            Assert.assertTrue((Boolean) now.isBefore(new DateTime().withTimeZone("UTC")
                .plusHours(-1)
                .getTime()));
        }
        else
        {
            Assert.assertTrue((Boolean) now.isAfter(new DateTime().withTimeZone("UTC")
                .plusHours(-1)
                .getTime()));
        }
    }

    @Test
    public void format()
    {
        SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        Assert.assertEquals(df.format(now.toDate()), now.format("hh:mm:ss"));
    }

    @Test
    public void timeZone()
    {
        Assert.assertEquals(TimeZone.getTimeZone("UTC").getDisplayName(), now.getTimeZone());
    }

    @Test
    public void plusSeconds()
    {
        Assert.assertEquals((Calendar.getInstance().get(Calendar.SECOND) + 1) % 60, now.plusSeconds(1)
            .getSeconds());
    }

    @Test
    public void plusSecondsRollover()
    {
        Assert.assertEquals(1, now.plusHours(172800).toCalendar().get(Calendar.DAY_OF_YEAR));
    }

    @Test
    public void plusMinutes()
    {
        Assert.assertEquals((Calendar.getInstance().get(Calendar.MINUTE) + 1) % 60, now.plusMinutes(1)
            .getMinutes());
    }

    @Test
    public void plusMinutesRollover()
    {
        Assert.assertEquals(1, now.plusHours(2880).toCalendar().get(Calendar.DAY_OF_YEAR));
    }

    @Test
    public void plusHours()
    {
        Assert.assertEquals(
            (Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.HOUR_OF_DAY) + 1) % 24,
            now.plusHours(1).getHours());
    }

    @Test
    public void plusHoursRollover()
    {
        Assert.assertEquals(1, now.plusHours(48).toCalendar().get(Calendar.DAY_OF_YEAR));
    }

    @Test
    public void withTimeZone()
    {
        int hour = now.getHours();
        assertEquals(hour, now.withTimeZone("GMT-03:00").getHours());
    }

    @Test
    public void changeTimeZone()
    {
        int hour = now.getHours();
        assertEquals((hour + 24 - 3) % 24, now.changeTimeZone("GMT-03:00").getHours());
    }

    @Test
    public void changeTimeZoneRollover()
    {
        Assert.assertEquals(1, now.plusHours(48).toCalendar().get(Calendar.DAY_OF_YEAR));
    }

    @Test
    @Ignore("MULE-7771/MULE-6926: flaky test")
    public void seconds()
    {
        assertEquals(Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.SECOND), now.getSeconds());
    }

    @Test
    public void minutes()
    {
        assertEquals(Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.MINUTE), now.getMinutes());
    }

    @Test
    public void hourOfDay()
    {
        assertEquals(Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.HOUR_OF_DAY),
            now.getHours());
    }

    @Test
    public void testToString()
    {
        assertEquals(DatatypeConverter.printTime(Calendar.getInstance(TimeZone.getTimeZone("UTC")))
            .substring(0, 8), now.withTimeZone("UTC").toString().substring(0, 8));
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
