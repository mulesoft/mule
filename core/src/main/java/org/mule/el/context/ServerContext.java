/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.context;

import org.mule.util.NetworkUtils;

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
 */
public class ServerContext
{

    public static String getHost() throws UnknownHostException
    {
        return NetworkUtils.getLocalHost().getCanonicalHostName();
    }

    public static String getIp() throws UnknownHostException
    {
        return NetworkUtils.getLocalHost().getHostAddress();
    }

    public static TimeZone getTimeZone()
    {
        return Calendar.getInstance().getTimeZone();
    }

    public static Locale getLocale()
    {
        return Locale.getDefault();
    }

    public static String getTmpDir()
    {
        return System.getProperty("java.io.tmpdir");
    }

    public static String getFileSeparator()
    {
        return System.getProperty("file.separator");
    }

    public static Map<String, String> getEnv()
    {
        return System.getenv();
    }

    public static Properties getSystemProperties()
    {
        return System.getProperties();
    }

    public static String getOsName()
    {
        return System.getProperty("os.name");
    }

    public static String getOsArch()
    {
        return System.getProperty("os.arch");
    }

    public  static String getOsVersion()
    {
        return System.getProperty("os.version");
    }

    public static String getJavaVersion()
    {
        return System.getProperty("java.version");
    }

    public static String getJavaVendor()
    {
        return System.getProperty("java.vendor");
    }

    public static String getUserName()
    {
        return System.getProperty("user.name");
    }

    public static String getUserHome()
    {
        return System.getProperty("user.home");
    }

    public static String getUserDir()
    {
        return System.getProperty("user.dir");
    }
    
}
