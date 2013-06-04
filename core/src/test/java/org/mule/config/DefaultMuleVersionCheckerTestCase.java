/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

import org.mule.api.config.ConfigurationException;
import org.mule.api.config.MuleVersionChecker;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Field;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class DefaultMuleVersionCheckerTestCase
{

    private MuleVersionChecker checker;

    @Before
    public void setUp()
    {
        Manifest manifest = Mockito.mock(Manifest.class);

        Attributes attributes = Mockito.mock(Attributes.class);
        Mockito.when(manifest.getMainAttributes()).thenReturn(attributes);
        Mockito.when(attributes.getValue(Mockito.any(Attributes.Name.class))).thenReturn("3.4");

        Field manifestField = null;
        try
        {
            manifestField = MuleManifest.class.getDeclaredField("manifest");
        }
        catch (NoSuchFieldException e)
        {
            throw new RuntimeException("Could not access manifest field to create mock", e);
        }
        manifestField.setAccessible(true);
        try
        {
            manifestField.set(null, manifest);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException("Could not set manifest field", e);
        }

        this.checker = new DefaultMuleVersionChecker();
    }

    @Test
    public void currentVersion()
    {
        Assert.assertEquals(this.checker.getMuleVersion(), MuleManifest.getProductVersion());
    }

    @Test(expected=ConfigurationException.class)
    public void assertGreaterMajorVersion() throws ConfigurationException
    {
        String version = this.addVersion(this.getVersion(), "1.0.0");
        this.checker.assertRuntimeGreaterOrEquals(version);
        Assert.fail("Was expecting a failure do to major mule version");
    }

    @Test
    public void assertLowerMajorVersion()
    {
        String version = this.addVersion(this.getVersion(), "-1.0.0");
        try
        {
            this.checker.assertRuntimeGreaterOrEquals(version);
        }
        catch (ConfigurationException e)
        {
            Assert.fail("was expecting failure due to minor mule version");
        }
    }

    @Test
    public void assertLowerMinorVersion()
    {
        String version = this.addVersion(this.getVersion(), "0.-1.0");
        try
        {
            this.checker.assertRuntimeGreaterOrEquals(version);
        }
        catch (ConfigurationException e)
        {
            Assert.fail("was expecting failure due to minor mule version");
        }
    }

    @Test(expected=ConfigurationException.class)
    public void assertGreaterMinorVersion() throws ConfigurationException
    {
        String version = this.addVersion(this.getVersion(), "0.1.0");
        this.checker.assertRuntimeGreaterOrEquals(version);
        Assert.fail("was expecting failure due to minor mule version");
    }

    @Test
    public void assertLowerPatchVersion()
    {
        String version = this.addVersion(this.getVersion(), "0.0.-1");
        try
        {
            this.checker.assertRuntimeGreaterOrEquals(version);
        }
        catch (ConfigurationException e)
        {
            Assert.fail("was expecting failure due to minor mule version");
        }
    }

    @Test(expected=ConfigurationException.class)
    public void assertGreaterPatchVersion() throws ConfigurationException
    {
        String version = this.addVersion(this.getVersion(), "0.0.1");
        this.checker.assertRuntimeGreaterOrEquals(version);
        Assert.fail("was expecting failure due to minor mule version");
    }

    @Test
    public void assertEqualMuleVersion()
    {
        try
        {
            this.checker.assertRuntimeGreaterOrEquals(this.getVersion());
        }
        catch (ConfigurationException e)
        {
            Assert.fail("current mule version should be valid");
        }
    }

    private String addVersion(String version, String delta)
    {
        String[] currentRuntimeVersion = version.split("\\.");
        String[] deltaVersion = delta.split("\\.");

        StringBuilder newVersion = new StringBuilder();

        int i = 0;
        for (; i < currentRuntimeVersion.length && i < deltaVersion.length; i++)
        {

            if (newVersion.length() > 0)
            {
                newVersion.append('.');
            }

            int versionDigit = Integer.parseInt(currentRuntimeVersion[i]);
            int deltaDigit = Integer.parseInt(deltaVersion[i]);

            newVersion.append(versionDigit + deltaDigit);
        }

        for (; i < currentRuntimeVersion.length; i++)
        {
            newVersion.append(".0");
        }
        
        for (; i < deltaVersion.length; i++) {
            newVersion.append('.').append(deltaVersion[i]);
        }

        return newVersion.toString();
    }

    private String getVersion()
    {
        return this.checker.getMuleVersion();
    }

}
