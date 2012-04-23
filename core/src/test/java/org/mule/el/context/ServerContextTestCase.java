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
        Assert.assertEquals(System.getProperties(), evaluate("server.systemProperties"));
    }

    public void assignValueToJavaSystemProperties()
    {
        assertImmutableVariable("server.systemProperties='1'");
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
        Assert.assertEquals(System.getProperty("file.separator"), evaluate("server.fileSeparator"));
    }

    public void assignValueToFileseperator()
    {
        assertImmutableVariable("server.fileSeparator='1'");
    }

    @Test
    public void osName()
    {
        Assert.assertEquals(System.getProperty("os.name"), evaluate("server.osName"));
    }

    public void assignValueToOsName()
    {
        assertImmutableVariable("server.osName='1'");
    }

    @Test
    public void osArch()
    {
        Assert.assertEquals(System.getProperty("os.arch"), evaluate("server.osArch"));
    }

    public void assignValueToOsArch()
    {
        assertImmutableVariable("server.osArch='1'");
    }

    @Test
    public void osVersion()
    {
        Assert.assertEquals(System.getProperty("os.version"), evaluate("server.osVersion"));
    }

    public void assignValueToOsVersion()
    {
        assertImmutableVariable("server.os.version='1'");
    }

    @Test
    public void javaVersion()
    {
        Assert.assertEquals(System.getProperty("java.version"), evaluate("server.javaVersion"));
    }

    public void assignValueToJavaVersion()
    {
        assertImmutableVariable("server.javaVersion='1'");
    }

    @Test
    public void javaVendor()
    {
        Assert.assertEquals(System.getProperty("java.vendor"), evaluate("server.javaVendor"));
    }

    public void assignValueToJavaVendor()
    {
        assertImmutableVariable("server.javaVendor='1'");
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

    @Test
    public void userName()
    {
        Assert.assertEquals(System.getProperty("user.name"), evaluate("server.userName"));
    }

    public void assignValueToUserName()
    {
        assertImmutableVariable("server.userName='1'");
    }

    @Test
    public void userHome()
    {
        Assert.assertEquals(System.getProperty("user.home"), evaluate("server.userHome"));
    }

    public void assignValueToUserHome()
    {
        assertImmutableVariable("server.userHome='1'");
    }

    @Test
    public void userDir()
    {
        Assert.assertEquals(System.getProperty("user.dir"), evaluate("server.userDir"));
    }

    public void assignValueToUseDir()
    {
        assertImmutableVariable("server.userDir='1'");
    }

}
