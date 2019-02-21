/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.reboot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.util.FileUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;

public class MuleContainerBootstrapUtilsTestCase
{

    @Before
    public void setUp()
    {
        System.setProperty("mule.home", "foo");
    }

    /**
     * Test method for {@link org.mule.module.reboot.MuleContainerBootstrapUtils#isStandalone()}.
     */
    @Test
    public void testIsStandaloneTrue()
    {        
        assertTrue(MuleContainerBootstrapUtils.isStandalone());
    }

    /**
     * Test method for {@link org.mule.module.reboot.MuleContainerBootstrapUtils#getMuleHome()}.
     */
    @Test
    public void testGetMuleHomeFile()
    {
        File muleHome = MuleContainerBootstrapUtils.getMuleHome();
        assertNotNull(muleHome.getAbsolutePath());
    }

    /**
     * Test method for {@link org.mule.module.reboot.MuleContainerBootstrapUtils#getMuleAppsDir()}.
     */
    @Test
    public void testGetMuleAppsFile()
    {
        File muleApps = MuleContainerBootstrapUtils.getMuleAppsDir();
        assertNotNull(muleApps.getAbsolutePath());
    }

    /**
     * Test method for {@link org.mule.module.reboot.MuleContainerBootstrapUtils#getMuleLibDir()}.
     */
    @Test
    public void testGetMuleLibDir()
    {   File muleLib = MuleContainerBootstrapUtils.getMuleLibDir();
        assertNotNull(muleLib.getAbsolutePath());
    }

    /**
     * Test method for {@link org.mule.module.reboot.MuleContainerBootstrapUtils#getMuleLocalJarFile()}.
     */
    @Test
    public void testGetMuleLocalJarFile()
    {   File muleLocalJar = MuleContainerBootstrapUtils.getMuleLocalJarFile(); 
        assertNotNull(muleLocalJar.getAbsolutePath());
    }

    /**
     * Test method for {@link org.mule.module.reboot.MuleContainerBootstrapUtils#getResource(java.lang.String, java.lang.Class)}.
     * @throws IOException 
     */
    @Test
    public void testGetResource() throws IOException
    {
        URL resource = MuleContainerBootstrapUtils.getResource("test-resource.txt", this.getClass());        
        assertNotNull(resource);
        Object content = resource.getContent();
        assertTrue(content instanceof InputStream);
        BufferedReader in = new BufferedReader(new InputStreamReader((InputStream)content));
        assertEquals("msg=Hello World", in.readLine());
    }

    /**
     * Test method for {@link org.mule.module.reboot.MuleContainerBootstrapUtils#renameFile(java.io.File, java.io.File)}.
     * @throws IOException 
     */
    @Test
    public void testRenameFile() throws IOException
    {
        File source = File.createTempFile("foo", ".tmp");
        File dest = new File(source.getParent() + File.separatorChar + "dest" + System.currentTimeMillis() + ".tmp");
        assertFalse(dest.exists());
        MuleContainerBootstrapUtils.renameFile(source, dest);
        assertTrue(dest.exists());
        assertFalse(source.exists());
        FileUtils.deleteFile(source);
        FileUtils.deleteFile(dest);
    }

    /**
     * Test method for {@link org.mule.module.reboot.MuleContainerBootstrapUtils#renameFileHard(java.io.File, java.io.File)}.
     * @throws IOException 
     */
    @Test
    public void testRenameFileHard() throws IOException
    {
        File source = File.createTempFile("foo2", ".tmp");
        File dest = new File(source.getParent() + File.separatorChar + "dest2" + System.currentTimeMillis() + ".tmp");
        assertFalse(dest.exists());
        MuleContainerBootstrapUtils.renameFileHard(source, dest);
        assertTrue(dest.exists());
        assertFalse(source.exists());
        FileUtils.deleteFile(source);
        FileUtils.deleteFile(dest);
    }

    /**
     * Test method for {@link org.mule.module.reboot.MuleContainerBootstrapUtils#copy(java.io.InputStream, java.io.OutputStream)}.
     * @throws IOException 
     */
    @Test
    public void testCopy() throws IOException
    {
        byte[] b = {0,1,2};
        ByteArrayInputStream input = new ByteArrayInputStream(b, 0, Integer.MAX_VALUE);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int i = MuleContainerBootstrapUtils.copy(input, output);
        assertEquals(b.length, i);
    }

    /**
     * Test method for {@link org.mule.module.reboot.MuleContainerBootstrapUtils#copyLarge(java.io.InputStream, java.io.OutputStream)}.
     * @throws IOException 
     */
    @Test
    public void testCopyLarge() throws IOException
    {
        byte[] b = {0,1,2};
        ByteArrayInputStream input = new ByteArrayInputStream(b, 0, Integer.MAX_VALUE);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        long i = MuleContainerBootstrapUtils.copyLarge(input, output);
        assertEquals(b.length, i);
    }
}


