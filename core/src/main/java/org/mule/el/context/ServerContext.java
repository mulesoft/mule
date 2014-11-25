/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.context;

import org.mule.el.datetime.DateTime;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Exposes information about both the physical server and Java Runtime Environment (JRE) that Mule is running on:
 * 
 * <li> <b>env</b>              <i>Map of Operating System environment variables </i>
 * <li> <b>fileSeparator</b>    <i>Character that separates components of a file path. This is "/" on UNIX and "\" on Windows.</i>
 * <li> <b>host</b>             <i>Fully qualified domain name for the server</i>
 * <li> <b>ip</b>               <i>The IP address of the server</i>
 * <li> <b>locale</b>           <i>The default locale (java.util.Locale) of the JRE.  Can be used language (locale.language), country (locale.country) and  </i>
 * <li> <b>javaVersion</b>      <i>JRE version</i>
 * <li> <b>javaVendor</b>       <i>JRE vendor name</i>
 * <li> <b>osName</b>           <i>Operating System name</i>
 * <li> <b>osArch</b>           <i>Operating System architecture</i>
 * <li> <b>osVersion</b>        <i>Operating System version</i>
 * <li> <b>systemProperties</b> <i>Map of Java system properties</i>
 * <li> <b>timeZone</b>         <i>Default TimeZone (java.util.TimeZone) of the JRE.</i>
 * <li> <b>tmpDir</b>           <i>Temporary directory for use by the JRE</i>
 * <li> <b>userName</b>         <i>User name</i>
 * <li> <b>userHome</b>         <i>User home directory</i>
 * <li> <b>userDir</b>          <i>User working directory</i>
 * <li> <b>nanoSeconds</b>      <i>Current system time in nanoseconds</i>
 * <li> <b>dateTime</b>          <i>Current system time via a DateTime utility object (see below)</i>
 * 
 * <b>dateTime</b>
 * 
 * <li> <b>milliSeconds, seconds, minutes, hours</b>  <i>Integer values for milliSeconds, seconds and minutes.</i>
 * <li> <b>dayOfWeek, dayOfMonth, dayOfYear</b>       <i>Integer value for day of week, month and year.</i>
 * <li> <b>weekOfMonth, weekOfYear</b>                <i>Integer value for week of month and year</i>
 * <li> <b>month</b>                                  <i>Integer value for month of year</i>
 * <li> <b>zone</b>                                   <i>String. The TimeZone display name.</i>
 * <li> <b>withTimeZone('timeZoneString')</b>         <i>Changes TimeZone to that specified using TimeZone string identifier.
 *                                                       Returns DateTime for method chaining. (Does not alter system timeZone or affect other uses of server.dateTime)</i>
 * <li> <b>withLocale('localeString')</b>             <i>Changes DateTime Locale to that specified using Locale string identifier. Returns DateTime for method chaining.</i>
 *                                                       Returns DateTime for method chaining. (Does not alter system locale or affect other uses of server.dateTime)</i>
 * <li> <b>isBefore(DateTimeContext date)</b>         <i>Boolean.  Returns true if the date parameter is before the current DateTime.</i>
 * <li> <b>isAfter(DateTimeContext date)</b>          <i>Boolean.  Returns true if the date parameter is after the current DateTime.</i>
 * <li> <b>addSeconds(int seconds)</b>                <i>Add n seconds to the current DateTime. Returns DateTime for method chaining.</i>
 * <li> <b>addMinutes(int minutes)</b>                <i>Add n minutes to the current DateTime. Returns DateTime for method chaining.</i>
 * <li> <b>addHours(int hours)</b>                    <i>Add n hours to the current DateTime. Returns DateTime for method chaining.</i>
 * <li> <b>addDay(int days)</b>                       <i>Add n days to the current DateTime. Returns DateTime for method chaining.</i>
 * <li> <b>addWeeks(int weeks)</b>                    <i>Add n weeks to the current DateTime. Returns DateTime for method chaining.</i>
 * <li> <b>addMonths(int months)</b>                  <i>Add n months to the current DateTime. Returns DateTime for method chaining.</i>
 * <li> <b>addYears(int years)</b>                    <i>Add n years to the current DateTime. Returns DateTime for method chaining.</i>
 **/
public class ServerContext
{

    // Get values here to avoid contention in System.getProperty() in runtime
    private static final String tmpDir = System.getProperty("java.io.tmpdir");
    private static final String fileSeparator = System.getProperty("file.separator");
    private static final String osName = System.getProperty("os.name");
    private static final String osArch = System.getProperty("os.arch");
    private static final String osVersion = System.getProperty("os.version");
    private static final String javaVersion = System.getProperty("java.version");
    private static final String javaVendor = System.getProperty("java.vendor");
    private static final String userName = System.getProperty("user.name");
    private static final String userHome = System.getProperty("user.home");
    private static final String userDir = System.getProperty("user.dir");

    public String getHost() throws UnknownHostException
    {
        return InetAddress.getLocalHost().getCanonicalHostName();
    }

    public String getIp() throws UnknownHostException
    {
        return InetAddress.getLocalHost().getHostAddress();
    }

    public TimeZone getTimeZone()
    {
        return Calendar.getInstance().getTimeZone();
    }

    public Locale getLocale()
    {
        return Locale.getDefault();
    }

    public String getTmpDir()
    {
        return tmpDir ;
    }

    public String getFileSeparator()
    {
        return fileSeparator;
    }

    public Map<String, String> getEnv()
    {
        return System.getenv();
    }

    public Properties getSystemProperties()
    {
        return System.getProperties();
    }

    public String getOsName()
    {
        return osName;
    }

    public String getOsArch()
    {
        return osArch;
    }

    public String getOsVersion()
    {
        return osVersion;
    }

    public String getJavaVersion()
    {
        return javaVersion;
    }

    public String getJavaVendor()
    {
        return javaVendor;
    }

    public String getUserName()
    {
        return userName;
    }

    public String getUserHome()
    {
        return userHome;
    }

    public String getUserDir()
    {
        return userDir;
    }

    public DateTime getDateTime()
    {
        return new DateTime();
    }

    public long nanoTime()
    {
        return System.nanoTime();
    }
}
