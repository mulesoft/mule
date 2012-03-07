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

    public void assignValueToHost()
    {
        assertImmutableVariable("server.host='1'");
    }

    @Test
    public void ip() throws UnknownHostException
    {
        Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), evaluate("server.ip"));
    }

    public void assignValueToIp()
    {
        assertImmutableVariable("server.ip='1'");
    }

    @Test
    public void javaSystemProperties()
    {
        Assert.assertEquals(System.getProperties(), evaluate("server.java.properties"));
    }

    public void assignValueToJavaSystemProperties()
    {
        assertImmutableVariable("server.java.properties='1'");
    }

    @Test
    public void tmpDir()
    {
        Assert.assertEquals(System.getProperty("java.io.tmpdir"), evaluate("server.tmpDir"));
    }

    public void assignValueToTmpdir()
    {
        assertImmutableVariable("server.tmpDir='1'");
    }

    @Test
    public void fileSeperator()
    {
        Assert.assertEquals(System.getProperty("file.separator"), evaluate("server.fileSeperator"));
    }

    public void assignValueToFileseperator()
    {
        assertImmutableVariable("server.fileSeperator='1'");
    }

    @Test
    public void user()
    {
        Assert.assertEquals(System.getProperty("user.name"), evaluate("server.user"));
    }

    public void assignValueUser()
    {
        assertImmutableVariable("server.user='1'");
    }

    @Test
    public void os()
    {
        evaluate("server.os");
    }

    public void assignValueToOs()
    {
        assertImmutableVariable("server.os='1'");
    }

    @Test
    public void osName()
    {
        Assert.assertEquals(System.getProperty("os.name"), evaluate("server.os.name"));
    }

    public void assignValueToOsName()
    {
        assertImmutableVariable("server.os.name='1'");
    }

    @Test
    public void osArch()
    {
        Assert.assertEquals(System.getProperty("os.arch"), evaluate("server.os.arch"));
    }

    public void assignValueToOsArch()
    {
        assertImmutableVariable("server.os.arch='1'");
    }

    @Test
    public void osVersion()
    {
        Assert.assertEquals(System.getProperty("os.version"), evaluate("server.os.version"));
    }

    public void assignValueToOsVersion()
    {
        assertImmutableVariable("server.os.version='1'");
    }

    @Test
    public void java()
    {
        evaluate("server.java");
    }

    public void assignValueToJava()
    {
        assertImmutableVariable("server.java='1'");
    }

    @Test
    public void javaVersion()
    {
        Assert.assertEquals(System.getProperty("java.version"), evaluate("server.java.version"));
    }

    public void assignValueToJavaVersion()
    {
        assertImmutableVariable("server.java.version='1'");
    }

    @Test
    public void javaVendor()
    {
        Assert.assertEquals(System.getProperty("java.vendor"), evaluate("server.java.vendor"));
    }

    public void assignValueToJavaVendor()
    {
        assertImmutableVariable("server.java.vendor='1'");
    }

    @Test
    public void env()
    {
        Assert.assertEquals(System.getenv(), evaluate("server.env"));
    }

    public void assignValueToEnv()
    {
        assertImmutableVariable("server.env='1'");
    }

    @Test
    public void timeZone()
    {
        Assert.assertEquals(Calendar.getInstance().getTimeZone(), evaluate("server.timeZone"));
    }

    public void assignValueToTimeZone()
    {
        assertImmutableVariable("server.timeZone='1'");
    }

    @Test
    public void locale()
    {
        Assert.assertEquals(Locale.getDefault(), evaluate("server.locale"));
    }

    public void assignValueToLocal()
    {
        assertImmutableVariable("server.locale='1'");
    }

}
