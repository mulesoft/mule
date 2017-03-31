/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import org.mule.runtime.core.util.NetworkUtils;
import org.mule.runtime.core.util.SystemUtils;

import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Exposes information about both the physical server and Java Runtime Environment (JRE) that Mule is running on:
 * 
 * <li><b>env</b> <i>Map of Operating System environment variables </i>
 * <li><b>fileSeparator</b> <i>Character that separates components of a file path. This is "/" on UNIX and "\" on Windows.</i>
 * <li><b>host</b> <i>Fully qualified domain name for the server</i>
 * <li><b>ip</b> <i>The IP address of the server</i>
 * <li><b>locale</b> <i>The default locale (java.util.Locale) of the JRE. Can be used language (locale.language), country
 * (locale.country) and </i>
 * <li><b>javaVersion</b> <i>JRE version</i>
 * <li><b>javaVendor</b> <i>JRE vendor name</i>
 * <li><b>osName</b> <i>Operating System name</i>
 * <li><b>osArch</b> <i>Operating System architecture</i>
 * <li><b>osVersion</b> <i>Operating System version</i>
 * <li><b>systemProperties</b> <i>Map of Java system properties</i>
 * <li><b>timeZone</b> <i>Default TimeZone (java.util.TimeZone) of the JRE.</i>
 * <li><b>tmpDir</b> <i>Temporary directory for use by the JRE</i>
 * <li><b>userName</b> <i>User name</i>
 * <li><b>userHome</b> <i>User home directory</i>
 * <li><b>userDir</b> <i>User working directory</i>
 **/
public class ServerContext {

  public String getHost() throws UnknownHostException {
    return NetworkUtils.getLocalHost().getCanonicalHostName();
  }

  public String getIp() throws UnknownHostException {
    return NetworkUtils.getLocalHost().getHostAddress();
  }

  public TimeZone getTimeZone() {
    return Calendar.getInstance().getTimeZone();
  }

  public Locale getLocale() {
    return Locale.getDefault();
  }

  public String getTmpDir() {
    return SystemUtils.JAVA_IO_TMPDIR;
  }

  public String getFileSeparator() {
    return SystemUtils.FILE_SEPARATOR;
  }

  public Map<String, String> getEnv() {
    return System.getenv();
  }

  public Properties getSystemProperties() {
    return System.getProperties();
  }

  public String getOsName() {
    return SystemUtils.OS_NAME;
  }

  public String getOsArch() {
    return SystemUtils.OS_ARCH;
  }

  public String getOsVersion() {
    return SystemUtils.OS_VERSION;
  }

  public String getJavaVersion() {
    return SystemUtils.JAVA_VERSION;
  }

  public String getJavaVendor() {
    return SystemUtils.JAVA_VENDOR;
  }

  public String getUserName() {
    return SystemUtils.USER_NAME;
  }

  public String getUserHome() {
    return SystemUtils.USER_HOME;
  }

  public String getUserDir() {
    return SystemUtils.USER_DIR;
  }

}
