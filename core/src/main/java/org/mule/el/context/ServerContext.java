/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.context;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Exposes information about 
 * 
 * <li> <b>env</b>             <i>Map of enviroment variables </i>
 * <li> <b>fileseperator</b>   <i></i>
 * <li> <b>host</b>            <i></i>
 * <li> <b>ip</b>              <i></i>
 * <li> <b>locale</b>          <i></i>
 * <li> <b>java.properties</b> <i>Map of Java system properties</i>
 * <li> <b>java.version</b>    <i></i>
 * <li> <b>java.vendor</b>     <i></i>
 * <li> <b>os.name</b>         <i></i>
 * <li> <b>os.arch</b>         <i></i>
 * <li> <b>os.version</b>      <i></i>
 * <li> <b>timezone</b>        <i></i>
 * <li> <b>tmpdir</b>          <i></i>
 * <li> <b>user</b>            <i></i>
 */
public class ServerContext
{

    public static String getHost() throws UnknownHostException
    {
        return InetAddress.getLocalHost().getCanonicalHostName();
    }

    public static String getIp() throws UnknownHostException
    {
        return InetAddress.getLocalHost().getHostAddress();
    }

    public static OperatingSystemContext getOs() throws UnknownHostException
    {
        return new OperatingSystemContext();
    }

    public static JAVARuntimeContext getJava()
    {
        return new JAVARuntimeContext();
    }

    public static TimeZone getTimeZone()
    {
        return Calendar.getInstance().getTimeZone();
    }

    public static Locale getLocale()
    {
        return Locale.getDefault();
    }

    public String getTmpDir()
    {
        return System.getProperty("java.io.tmpdir");
    }

    public String getFileSeperator()
    {
        return System.getProperty("file.separator");
    }

    public String getUser()
    {
        return System.getProperty("user.name");
    }

    public Map<String, String> getEnv()
    {
        return System.getenv();
    }

    public static class OperatingSystemContext
    {
        public String getName()
        {
            return System.getProperty("os.name");
        }

        public String getArch()
        {
            return System.getProperty("os.arch");
        }

        public String getVersion()
        {
            return System.getProperty("os.version");
        }

        public String toString()
        {
            return getName() + " (" + getVersion() + ", " + getArch() + ")";
        }
    }

    public static class JAVARuntimeContext
    {
        public String getVersion()
        {
            return System.getProperty("java.version");
        }

        public String getVendor()
        {
            return System.getProperty("java.vendor");
        }

        public Properties getProperties()
        {
            return System.getProperties();
        }

        public String toString()
        {
            return getVersion() + " (" + getVendor() + ")";
        }
    }

}
