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

import org.mule.api.MuleRuntimeException;
import org.mule.el.AbstractELTestCase;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Locale;

import junit.framework.Assert;

import org.junit.Test;

public class ServerContextTestCase extends AbstractELTestCase
{
    public ServerContextTestCase(Variant variant)
    {
        super(variant);
    }

    @Test
    public void host() throws UnknownHostException
    {
        Assert.assertEquals(InetAddress.getLocalHost().getCanonicalHostName(), evaluate("server.host"));
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignValueToHost()
    {
        evaluate("server.host='1'");
    }

    @Test
    public void ip() throws UnknownHostException
    {
        Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), evaluate("server.ip"));
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignValueToIp()
    {
        evaluate("server.ip='1'");
    }

    @Test
    public void javaSystemProperties()
    {
        Assert.assertEquals(System.getProperties(), evaluate("server.java.properties"));
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignValueToJavaSystemProperties()
    {
        evaluate("server.java.properties='1'");
    }

    @Test
    public void tmpDir()
    {
        Assert.assertEquals(System.getProperty("java.io.tmpdir"), evaluate("server.tmpDir"));
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignValueToTmpdir()
    {
        evaluate("server.tmpDir='1'");
    }

    @Test
    public void fileSeperator()
    {
        Assert.assertEquals(System.getProperty("file.separator"), evaluate("server.fileSeperator"));
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignValueToFileseperator()
    {
        evaluate("server.fileSeperator='1'");
    }

    @Test
    public void user()
    {
        Assert.assertEquals(System.getProperty("user.name"), evaluate("server.user"));
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignValueUser()
    {
        evaluate("server.user='1'");
    }

    @Test
    public void os()
    {
        evaluate("server.os");
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignValueToOs()
    {
        evaluate("server.os='1'");
    }

    @Test
    public void osName()
    {
        Assert.assertEquals(System.getProperty("os.name"), evaluate("server.os.name"));
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignValueToOsName()
    {
        evaluate("server.os.name='1'");
    }

    @Test
    public void osArch()
    {
        Assert.assertEquals(System.getProperty("os.arch"), evaluate("server.os.arch"));
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignValueToOsArch()
    {
        evaluate("server.os.arch='1'");
    }

    @Test
    public void osVersion()
    {
        Assert.assertEquals(System.getProperty("os.version"), evaluate("server.os.version"));
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignValueToOsVersion()
    {
        evaluate("server.os.version='1'");
    }

    @Test
    public void java()
    {
        evaluate("server.java");
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignValueToJava()
    {
        evaluate("server.java='1'");
    }

    @Test
    public void javaVersion()
    {
        Assert.assertEquals(System.getProperty("java.version"), evaluate("server.java.version"));
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignValueToJavaVersion()
    {
        evaluate("server.java.version='1'");
    }

    @Test
    public void javaVendor()
    {
        Assert.assertEquals(System.getProperty("java.vendor"), evaluate("server.java.vendor"));
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignValueToJavaVendor()
    {
        evaluate("server.java.vendor='1'");
    }

    @Test
    public void env()
    {
        Assert.assertEquals(System.getenv(), evaluate("server.env"));
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignValueToEnv()
    {
        evaluate("server.env='1'");
    }

    @Test
    public void timeZone()
    {
        Assert.assertEquals(Calendar.getInstance().getTimeZone(), evaluate("server.timeZone"));
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignValueToTimeZone()
    {
        evaluate("server.timeZone='1'");
    }

    @Test
    public void locale()
    {
        Assert.assertEquals(Locale.getDefault(), evaluate("server.locale"));
    }

    @Test(expected = MuleRuntimeException.class)
    public void assignValueToLocal()
    {
        evaluate("server.locale='1'");
    }

}
