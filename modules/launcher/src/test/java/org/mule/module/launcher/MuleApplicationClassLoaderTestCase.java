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
    private static final String RESOURCE_JUST_IN_DOMAIN = "test-resource-4.txt";

    private static final String DOMAIN_NAME = "test-domain";
    private static final String APP_NAME = "test-app";

    @Rule
    public TemporaryFolder tempMuleHome = new TemporaryFolder();

    private String previousMuleHome;

    private MuleSharedDomainClassLoader domainCL;
    private MuleApplicationClassLoader appCL;

    private File domainDir;
    private File classesDir;
    private File jarFile;

    @Before
    public void createAppClassLoader() throws IOException
    {
        // Create directories structure
        previousMuleHome = System.setProperty(MuleProperties.MULE_HOME_DIRECTORY_PROPERTY, tempMuleHome.getRoot().getAbsolutePath());

        domainDir = createDirectory("domains/%s", DOMAIN_NAME);

        classesDir = createDirectory("apps/%s/classes", APP_NAME);

        File libDir = tempMuleHome.newFolder(String.format("apps/%s/lib", APP_NAME));

        // Add jar file with resources in lib dir
        URL resourceSrcJarFile = Thread.currentThread().getContextClassLoader().getResource("test-jar-with-resources.jar");
        assertNotNull(resourceSrcJarFile);
        File srcJarFile = new File(resourceSrcJarFile.getFile());
        jarFile = new File(libDir, "test-jar-with-resources.jar");
        FileUtils.copyFile(srcJarFile, jarFile, false);

        // Add isolated resources in classes dir
        FileUtils.stringToFile(new File(classesDir, RESOURCE_IN_CLASSES_AND_JAR).getAbsolutePath(), "Some text");
        FileUtils.stringToFile(new File(classesDir, RESOURCE_JUST_IN_CLASSES).getAbsolutePath(), "Some text");

        // Add isolated resources in domain dir
        FileUtils.stringToFile(new File(domainDir, RESOURCE_JUST_IN_DOMAIN).getAbsolutePath(), "Some text");

        // Create app class loader
        domainCL = new MuleSharedDomainClassLoader(DOMAIN_NAME, Thread.currentThread().getContextClassLoader());
        appCL = new MuleApplicationClassLoader(APP_NAME, domainCL, null);
    }

    private File createDirectory(String format, Object... args)
    {
        File file = new File(tempMuleHome.getRoot(), String.format(format, args));
        file.mkdirs();
        return file;
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
    public void loadLocalResourcesOnlyFromExpectedDirectories() throws Exception
    {
        // Ensure all resources can be loaded from their respective locations using findResource
        assertLoadedFromClassesDir(appCL.findResource(RESOURCE_IN_CLASSES_AND_JAR));
        assertLoadedFromJarFile(appCL.findResource(RESOURCE_JUST_IN_JAR));
        assertLoadedFromClassesDir(appCL.findResource(RESOURCE_JUST_IN_CLASSES));
        assertNotLoaded(appCL.findResource(RESOURCE_JUST_IN_DOMAIN));
        assertLoadedFromDomainDir(domainCL.findResource(RESOURCE_JUST_IN_DOMAIN));

        // Ensure app resources are only loaded from classes directory
        assertLoadedFromClassesDir(appCL.findLocalResource(RESOURCE_IN_CLASSES_AND_JAR));
        assertNotLoaded(appCL.findLocalResource(RESOURCE_JUST_IN_JAR));
        assertLoadedFromClassesDir(appCL.findLocalResource(RESOURCE_JUST_IN_CLASSES));
        assertLoadedFromDomainDir(appCL.findLocalResource(RESOURCE_JUST_IN_DOMAIN));
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

    private void assertLoadedFromDomainDir(URL resource)
    {
        assertNotNull(resource);
        assertEquals("file", resource.getProtocol());
        assertTrue(resource.getFile().contains(domainDir.getAbsolutePath()));
    }

    private void assertNotLoaded(URL resource)
    {
        assertNull(resource);
    }
}