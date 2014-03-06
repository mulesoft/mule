/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.config.MuleProperties;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class MuleApplicationClassLoaderTestCase extends AbstractMuleTestCase
{

    private static final String RESOURCE_IN_CLASSES_AND_JAR = "test-resource-1.txt";
    private static final String RESOURCE_JUST_IN_JAR = "test-resource-2.txt";
    private static final String RESOURCE_JUST_IN_CLASSES = "test-resource-3.txt";

    private static final String APP_NAME = "appName";

    @Rule
    public TemporaryFolder tempMuleHome = new TemporaryFolder();

    private String previousMuleHome;

    private MuleApplicationClassLoader appCL;

    private File classesDir;
    private File jarFile;

    @Before
    public void createAppClassLoader() throws IOException
    {
        // Create directories structure
        previousMuleHome = System.setProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, tempMuleHome.getRoot().getAbsolutePath());
        classesDir = tempMuleHome.newFolder(String.format("apps/%s/classes", APP_NAME));
        File libDir = tempMuleHome.newFolder(String.format("apps/%s/lib", APP_NAME));

        // Add jar file with resources in lib dir
        jarFile = new File(libDir, "test-jar-with-resource.jar");
        URL resourceSrcJarFile = Thread.currentThread().getContextClassLoader().getResource("test-jar-with-resource.jar");
        assertNotNull(resourceSrcJarFile);
        File srcJarFile = new File(resourceSrcJarFile.getFile());
        FileUtils.copyFile(srcJarFile, jarFile, false);

        // Add isolated resources in classes dir
        FileUtils.stringToFile(new File(classesDir, RESOURCE_IN_CLASSES_AND_JAR).getAbsolutePath(), "Some text");
        FileUtils.stringToFile(new File(classesDir, RESOURCE_JUST_IN_CLASSES).getAbsolutePath(), "Some text");

        // Create app class loader
        appCL = new MuleApplicationClassLoader(APP_NAME, Thread.currentThread().getContextClassLoader());
    }

    @After
    public void cleanUp()
    {
        if (previousMuleHome != null)
        {
            System.setProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, previousMuleHome);
        }
        FileUtils.deleteTree(tempMuleHome.getRoot());
    }

    @Test
    public void loadAppResourcesOnlyFromClassesDirectory() throws Exception
    {
        // Ensure all resources can be loaded from their respective locations using findResource
        assertLoadedFromClassesDir(appCL.findResource(RESOURCE_IN_CLASSES_AND_JAR));
        assertLoadedFromJarFile(appCL.findResource(RESOURCE_JUST_IN_JAR));
        assertLoadedFromClassesDir(appCL.findResource(RESOURCE_JUST_IN_CLASSES));

        // Ensure app resources are only loaded from classes directory
        assertLoadedFromClassesDir(appCL.findArtifactResource(RESOURCE_IN_CLASSES_AND_JAR));
        assertNotLoaded(appCL.findArtifactResource(RESOURCE_JUST_IN_JAR));
        assertLoadedFromClassesDir(appCL.findArtifactResource(RESOURCE_JUST_IN_CLASSES));
    }

    private void assertLoadedFromClassesDir(URL resource)
    {
        assertNotNull(resource);
        assertEquals("file", resource.getProtocol());
        assertTrue(resource.getFile().contains(classesDir.getAbsolutePath()));
    }

    private void assertLoadedFromJarFile(URL resource)
    {
        assertNotNull(resource);
        assertEquals("jar", resource.getProtocol());
        assertTrue(resource.getFile().contains(jarFile.getAbsolutePath()));
    }

    private void assertNotLoaded(URL resource)
    {
        assertNull(resource);
    }

}
