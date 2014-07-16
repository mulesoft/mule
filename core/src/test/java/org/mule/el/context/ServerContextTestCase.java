/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.context;

import static org.junit.Assert.assertEquals;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.el.datetime.DateTime;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;

import junit.framework.Assert;

import org.apache.commons.lang.LocaleUtils;
import org.junit.Test;

public class ServerContextTestCase extends AbstractELTestCase
{
    public ServerContextTestCase(Variant variant, String mvelOptimizer)
    {
        super(variant, mvelOptimizer);
    }

    @Test
    public void host() throws UnknownHostException
    {
        Assert.assertEquals(InetAddress.getLocalHost().getCanonicalHostName(), evaluate("server.host"));
    }

    @Test
    public void assignValueToHost()
    {
        assertFinalProperty("server.host='1'");
    }

    @Test
    public void ip() throws UnknownHostException
    {
        Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), evaluate("server.ip"));
    }

    @Test
    public void assignValueToIp()
    {
        assertFinalProperty("server.ip='1'");
    }

    @Test
    public void javaSystemProperties()
    {
        Assert.assertEquals(System.getProperties(), evaluate("server.systemProperties"));
    }

    @Test
    public void assignValueToJavaSystemProperties()
    {
        assertFinalProperty("server.systemProperties='1'");
    }

    @Test
    public void tmpDir()
    {
        Assert.assertEquals(System.getProperty("java.io.tmpdir"), evaluate("server.tmpDir"));
    }

    @Test
    public void assignValueToTmpdir()
    {
        assertFinalProperty("server.tmpDir='1'");
    }

    @Test
    public void fileSeperator()
    {
        Assert.assertEquals(System.getProperty("file.separator"), evaluate("server.fileSeparator"));
    }

    @Test
    public void assignValueToFileseperator()
    {
        assertFinalProperty("server.fileSeparator='1'");
    }

    @Test
    public void osName()
    {
        Assert.assertEquals(System.getProperty("os.name"), evaluate("server.osName"));
    }

    @Test
    public void assignValueToOsName()
    {
        assertFinalProperty("server.osName='1'");
    }

    @Test
    public void osArch()
    {
        Assert.assertEquals(System.getProperty("os.arch"), evaluate("server.osArch"));
    }

    @Test
    public void assignValueToOsArch()
    {
        assertFinalProperty("server.osArch='1'");
    }

    @Test
    public void osVersion()
    {
        Assert.assertEquals(System.getProperty("os.version"), evaluate("server.osVersion"));
    }

    @Test
    public void assignValueToOsVersion()
    {
        assertFinalProperty("server.os.version='1'");
    }

    @Test
    public void javaVersion()
    {
        Assert.assertEquals(System.getProperty("java.version"), evaluate("server.javaVersion"));
    }

    @Test
    public void assignValueToJavaVersion()
    {
        assertFinalProperty("server.javaVersion='1'");
    }

    @Test
    public void javaVendor()
    {
        Assert.assertEquals(System.getProperty("java.vendor"), evaluate("server.javaVendor"));
    }

    @Test
    public void assignValueToJavaVendor()
    {
        assertFinalProperty("server.javaVendor='1'");
    }

    @Test
    public void env()
    {
        Assert.assertEquals(System.getenv(), evaluate("server.env"));
    }

    @Test
    public void assignValueToEnv()
    {
        assertFinalProperty("server.env='1'");
    }

    @Test
    public void timeZone()
    {
        Assert.assertEquals(Calendar.getInstance().getTimeZone(), evaluate("server.timeZone"));
    }

    @Test
    public void assignValueToTimeZone()
    {
        assertFinalProperty("server.timeZone='1'");
    }

    @Test
    public void locale()
    {
        Assert.assertEquals(Locale.getDefault(), evaluate("server.locale"));
    }

    @Test
    public void assignValueToLocal()
    {
        assertFinalProperty("server.locale='1'");
    }

    @Test
    public void userName()
    {
        Assert.assertEquals(System.getProperty("user.name"), evaluate("server.userName"));
    }

    @Test
    public void assignValueToUserName()
    {
        assertFinalProperty("server.userName='1'");
    }

    @Test
    public void userHome()
    {
        Assert.assertEquals(System.getProperty("user.home"), evaluate("server.userHome"));
    }

    @Test
    public void assignValueToUserHome()
    {
        assertFinalProperty("server.userHome='1'");
    }

    @Test
    public void userDir()
    {
        Assert.assertEquals(System.getProperty("user.dir"), evaluate("server.userDir"));
    }

    @Test
    public void assignValueToUserDir()
    {
        assertFinalProperty("server.userDir='1'");
    }

    @Test
    public void dateTime()
    {
        Assert.assertEquals(DateTime.class, evaluate("server.dateTime").getClass());
    }

    @Test
    public void assignValueToDateTime()
    {
        assertFinalProperty("server.dateTime='1'");
    }

    @Test
    public void dateTimeMilliSeconds()
    {
        Assert.assertTrue(((Long) evaluate("server.dateTime.milliSeconds")) < 1000);
    }

    @Test
    public void dateTimeIsBefore()
    {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        MuleMessage message = new DefaultMuleMessage(new DateTime(cal), muleContext);
        Assert.assertTrue((Boolean) evaluate("server.dateTime.isBefore(payload)", message));

    }

    @Test
    public void dateTimeIsAfter()
    {
        MuleMessage message = new DefaultMuleMessage(new DateTime(Calendar.getInstance()), muleContext);
        Assert.assertTrue((Boolean) evaluate("server.dateTime.isAfter(payload)", message));

    }

    @Test
    public void dateTimeFormat()
    {
        Assert.assertEquals(new SimpleDateFormat("EEE, MMM d, yyyy").format(new Date()),
            evaluate("server.dateTime.format('EEE, MMM d, yyyy')"));
        Assert.assertEquals(
            new SimpleDateFormat("EEE, MMM d, yyyy", LocaleUtils.toLocale("en_US")).format(new Date()),
            evaluate("server.dateTime.withLocale('en_US').format('EEE, MMM d, yyyy')"));
    }

    @Test
    public void dateTimeZone()
    {
        Assert.assertEquals(TimeZone.getDefault().getDisplayName(), evaluate("server.dateTime.timeZone"));
    }

    @Test
    public void dateTimeAddSeconds()
    {
        Assert.assertEquals((Calendar.getInstance().get(Calendar.SECOND) + 1) % 60,
            evaluate("(int) server.dateTime.plusSeconds(1).format('s')"));
    }

    @Test
    public void dateTimeAddMinutes()
    {
        Assert.assertEquals((Calendar.getInstance().get(Calendar.MINUTE) + 1) % 60,
            evaluate("server.dateTime.plusMinutes(1).minutes"));
    }

    @Test
    public void dateTimeAddHours()
    {
        Assert.assertEquals((Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 1) % 24,
            evaluate("server.dateTime.plusHours(1).hours"));
    }

    @Test
    public void dateTimeAddDays()
    {
        Assert.assertEquals((Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % 365) + 1,
            evaluate("(int) server.dateTime.plusDays(1).dayOfYear"));
    }

    @Test
    public void dateTimeAddWeeks()
    {
        Assert.assertEquals((Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) % 52) + 1,
            evaluate("(int) server.dateTime.plusWeeks(1).weekOfYear"));
    }

    @Test
    public void dateTimeAddMonths()
    {
        Assert.assertEquals(((Calendar.getInstance(Locale.US).get(Calendar.MONTH) + 1) % 12) + 1,
            evaluate("(int) server.dateTime.plusMonths(1).month"));
    }

    @Test
    public void dateTimeAddYears()
    {
        Assert.assertEquals(Calendar.getInstance(Locale.US).get(Calendar.YEAR) + 1,
            evaluate("(int) server.dateTime.plusYears(1).format('yyyy')"));
    }

    @Test
    public void dateTimeWithTimeZone()
    {
        assertEquals("Central European Time", evaluate("server.dateTime.withTimeZone('CET').timeZone"));
    }

    @Test
    public void dateTimeWithLocale()
    {
        assertEquals(new SimpleDateFormat("E").format(new Date()),
            evaluate("server.dateTime.withLocale('en_US').format('E')"));
        assertEquals(new SimpleDateFormat("E", LocaleUtils.toLocale("es_AR")).format(new Date()),
            evaluate("server.dateTime.withLocale('es_AR').format('E')"));
    }

    @Test
    public void dateTimeSeconds()
    {
        assertEquals(Calendar.getInstance().get(Calendar.SECOND), evaluate("server.dateTime.seconds"));
    }

    @Test
    public void dateTimeMinutes()
    {
        assertEquals(Calendar.getInstance().get(Calendar.MINUTE), evaluate("server.dateTime.minutes"));
    }

    @Test
    public void dateTimeHour()
    {
        assertEquals(Calendar.getInstance().get(Calendar.HOUR_OF_DAY), evaluate("server.dateTime.hours"));
    }

    @Test
    public void dateTimeDayOfWeek()
    {
        assertEquals(Calendar.getInstance().get(Calendar.DAY_OF_WEEK), evaluate("server.dateTime.dayOfWeek"));
    }

    @Test
    public void dateTimeDayOfMonth()
    {
        assertEquals(Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
            evaluate("server.dateTime.dayOfMonth"));
    }

    @Test
    public void dateTimeDayOfYear()
    {
        assertEquals(Calendar.getInstance().get(Calendar.DAY_OF_YEAR), evaluate("server.dateTime.dayOfYear"));
    }

    @Test
    public void dateTimeWeekOfMonth()
    {
        assertEquals(Calendar.getInstance().get(Calendar.WEEK_OF_MONTH),
            evaluate("server.dateTime.weekOfMonth"));
    }

    @Test
    public void dateTimeWeekOfYear()
    {
        assertEquals(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR),
            evaluate("server.dateTime.weekOfYear"));
    }

    @Test
    public void dateTimeMonthOfYear()
    {
        assertEquals(Calendar.getInstance().get(Calendar.MONTH) + 1, evaluate("server.dateTime.month"));
    }

    @Test
    public void dateTimeToString()
    {
        assertEquals(DatatypeConverter.printDateTime(Calendar.getInstance()).substring(0, 18),
            ((String) evaluate("server.dateTime").toString()).substring(0, 18));
    }

}
